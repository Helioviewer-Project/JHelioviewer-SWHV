package org.helioviewer.jhv;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JTabbedPane;

import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.component.MoviePanel;
import org.helioviewer.jhv.gui.dialog.AboutDialog;
import org.helioviewer.jhv.gui.dialog.MetaDataDialog;
import org.helioviewer.jhv.gui.dialog.SettingsDialog;
import org.helioviewer.jhv.io.ImageRequestSettings;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.timelines.draw.DrawController;

// Native window captures of the production app. Other platforms require an unobscured desktop.
public final class UiCapture {

    private static Path output;

    public static void main(String[] args) throws Exception {
        output = Path.of(args[0]);
        Files.createDirectories(output);
        JHelioviewer.main(new String[]{"-load", Path.of("extra/test/data/sample.171.fits").toAbsolutePath().toString()});
        try {
            AtomicBoolean ready = new AtomicBoolean();
            long deadline = System.nanoTime() + 90_000_000_000L;
            while (!ready.get() && System.nanoTime() < deadline) {
                EventQueue.invokeAndWait(() -> ready.set(MainFrame.get() != null && MainFrame.get().isShowing()
                        && Layers.getActiveImageLayer() != null && Layers.getActiveImageLayer().getImageData() != null));
                Thread.sleep(200);
            }
            if (!ready.get())
                throw new IllegalStateException("Production app did not display the local FITS image");
            capture("main", MainFrame.get());
            EventQueue.invokeAndWait(() -> MoviePanel.getInstance().setAdvanced(true));
            capture("playback", MainFrame.get());
            EventQueue.invokeAndWait(() -> findTabs(MainFrame.getLayersPanel()).setSelectedIndex(1));
            capture("overlays", MainFrame.get());
            dialog("settings", () -> new SettingsDialog().showDialog());
            dialog("about", () -> new AboutDialog().showDialog());
            dialog("metadata", () -> {
                MetaDataDialog metadata = MetaDataDialog.getInstance();
                metadata.setMetaData(Layers.getActiveImageLayer());
                metadata.showDialog();
            });
            dialog("new-layer", () -> MainFrame.getLayersPanel().getImageDialog().showNewLayerSelector());
            System.out.println("UI captures saved to " + output);
            System.exit(0);
        } catch (Throwable error) {
            error.printStackTrace();
            System.exit(1);
        }
    }

    private static JTabbedPane findTabs(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JTabbedPane tabs)
                return tabs;
            if (child instanceof Container container) {
                JTabbedPane tabs = findTabs(container);
                if (tabs != null)
                    return tabs;
            }
        }
        return null;
    }

    private static void dialog(String name, Runnable show) throws Exception {
        EventQueue.invokeLater(show);
        Thread.sleep(1000);
        Window[] selected = new Window[1];
        EventQueue.invokeAndWait(() -> {
            for (Window window : Window.getWindows()) {
                if (window instanceof JDialog && window.isShowing())
                    selected[0] = window;
            }
        });
        if (selected[0] == null)
            throw new IllegalStateException("Dialog did not open: " + name);
        capture(name, selected[0]);
        EventQueue.invokeAndWait(selected[0]::dispose);
    }

    private static void capture(String name, Window window) throws Exception {
        EventQueue.invokeAndWait(() -> {
            if (window == MainFrame.get()) {
                long end = Instant.parse("2012-09-01T00:00:00Z").toEpochMilli();
                ImageRequestSettings.instance().setInterval(end - 172800000L, end);
                DrawController.setSelectedInterval(end - 172800000L, end);
            }
            window.toFront();
            window.requestFocus();
        });
        Thread.sleep(700);
        Rectangle[] bounds = new Rectangle[1];
        EventQueue.invokeAndWait(() -> bounds[0] = window.getBounds());
        String finder = System.getProperty("jhv.capture.windowFinder");
        if (finder != null) {
            String title = window instanceof Frame frame ? frame.getTitle() : ((Dialog) window).getTitle();
            Process lookup = new ProcessBuilder(finder, Long.toString(ProcessHandle.current().pid()), title).inheritIO().redirectOutput(ProcessBuilder.Redirect.PIPE).start();
            String id = new String(lookup.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
            if (lookup.waitFor() != 0 || !id.matches("[0-9]+"))
                throw new IllegalStateException("Cannot identify native window: " + title);
            Process capture = new ProcessBuilder("/usr/sbin/screencapture", "-x", "-o", "-l", id,
                    output.resolve(name + ".png").toString()).inheritIO().start();
            if (capture.waitFor() != 0)
                throw new IllegalStateException("Native window capture failed: " + title);
        } else {
            ImageIO.write((BufferedImage) new Robot().createMultiResolutionScreenCapture(bounds[0]).getResolutionVariants().getLast(), "png", output.resolve(name + ".png").toFile());
        }
    }
}
