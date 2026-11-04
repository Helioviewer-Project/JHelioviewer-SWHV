package org.helioviewer.jhv;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.component.MoviePanel;
import org.helioviewer.jhv.gui.dialog.AboutDialog;
import org.helioviewer.jhv.gui.dialog.MetaDataDialog;
import org.helioviewer.jhv.gui.dialog.SettingsDialog;
import org.helioviewer.jhv.io.ImageRequestSettings;
import org.helioviewer.jhv.layers.GridLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.timelines.TimelineLayers;
import org.helioviewer.jhv.timelines.band.Band;
import org.helioviewer.jhv.timelines.draw.DrawController;

import org.json.JSONArray;
import org.json.JSONObject;

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
            captureTimeline();
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

    private static void captureTimeline() throws Exception {
        Band band = Band.deserialize(new JSONObject().put("bandType", new JSONObject().put("name", "Capture fixture (synthetic)")
                .put("label", "Capture fixture (synthetic)").put("unitLabel", "a.u.").put("range", new JSONArray().put(0).put(100))));
        long start = Instant.parse("2012-08-30T00:00:00Z").toEpochMilli();
        long[] dates = new long[2881];
        float[] values = new float[2881];
        for (int index = 0; index < dates.length; index++) {
            dates[index] = start + index * 60000L;
            double phase = (index - 1640) / 140.;
            values[index] = (float) (18 + 4 * Math.sin(index / 100.) + 65 * Math.exp(-phase * phase));
        }
        Method load = Band.class.getDeclaredMethod("addToCache", float[].class, long[].class);
        load.setAccessible(true);
        EventQueue.invokeAndWait(() -> {
            band.setDataColor(new Color(0x287fbd));
            TimelineLayers.add(band);
            try {
                load.invoke(band, values, dates);
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException(failure);
            }
            MoviePanel.getInstance().setAdvanced(false);
            findTabs(MainFrame.getLayersPanel()).setSelectedIndex(2);
        });
        Thread.sleep(1000);
        capture("timeline", MainFrame.get());
        EventQueue.invokeAndWait(() -> TimelineLayers.remove(band));
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
        long deadline = System.nanoTime() + 15_000_000_000L;
        do {
            captureWindow(name, window);
            if (window != MainFrame.get() || (hasRenderedScene(output.resolve(name + ".png"))
                    && (!name.equals("timeline") || hasTimelineCurve(output.resolve(name + ".png")))))
                return;
            Thread.sleep(200);
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException("Local AIA texture or coordinate grid absent from native capture: " + name);
    }

    private static boolean hasTimelineCurve(Path path) throws Exception {
        BufferedImage capture = ImageIO.read(path.toFile());
        int pixels = 0;
        for (int y = capture.getHeight() * 3 / 4; y < capture.getHeight() - 60; y++) {
            for (int x = capture.getWidth() / 2; x < capture.getWidth(); x++) {
                int rgb = capture.getRGB(x, y);
                int blue = rgb & 255;
                if (blue > ((rgb >> 16) & 255) + 40 && blue > ((rgb >> 8) & 255) + 10)
                    pixels++;
            }
        }
        return pixels > 100;
    }

    private static boolean hasRenderedScene(Path path) throws Exception {
        BufferedImage capture = ImageIO.read(path.toFile());
        // The fixture's image is golden. An empty canvas, green grid, or flat thumbnail cannot pass.
        Set<Integer> colors = new HashSet<>();
        int gridPixels = 0;
        for (int y = capture.getHeight() / 5; y < capture.getHeight() * 3 / 4; y += 2) {
            for (int x = capture.getWidth() / 2; x < capture.getWidth(); x += 2) {
                int rgb = capture.getRGB(x, y);
                int red = (rgb >> 16) & 255;
                int green = (rgb >> 8) & 255;
                int blue = rgb & 255;
                if (red > 40 && red > green && green > 2 * blue)
                    colors.add(rgb);
                if (green > 40 && green > red + 10 && green > blue + 10)
                    gridPixels++;
            }
        }
        return colors.size() > 128 && gridPixels > 50;
    }

    private static void checkSidebarWidth(Container parent) {
        JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, parent);
        for (Component child : parent.getComponents()) {
            if (!child.isShowing())
                continue;
            Rectangle bounds = SwingUtilities.convertRectangle(parent, child.getBounds(), viewport);
            if (bounds.x < 0 || bounds.x + bounds.width > viewport.getWidth())
                throw new IllegalStateException("Sidebar control clipped horizontally: " + child.getClass().getName() + " " + bounds);
            if (child instanceof Container container && !(child instanceof javax.swing.JTable))
                checkSidebarWidth(container);
        }
    }

    private static void captureWindow(String name, Window window) throws Exception {
        EventQueue.invokeAndWait(() -> {
            if (window == MainFrame.get()) {
                long end = Instant.parse("2012-09-01T00:00:00Z").toEpochMilli();
                ImageRequestSettings.instance().setInterval(end - 172800000L, end);
                DrawController.setSelectedInterval(end - 172800000L, end);
                for (Layer layer : Layers.getLayers()) {
                    if (layer instanceof GridLayer grid)
                        System.out.println(name + " grid: enabled=" + grid.isEnabled() + " lines=" + grid.getGridAlpha() + " labels=" + grid.getLabelAlpha());
                }
                DisplayController.display();
                checkSidebarWidth(MainFrame.getLayersPanel());
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
