package org.helioviewer.jhv.gui;

import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.ExitHooks;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.app.Commands;
import org.helioviewer.jhv.app.state.State;
import org.helioviewer.jhv.camera.Annotations;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.gui.dialogs.LoadStateDialog;
import org.helioviewer.jhv.gui.dialogs.ObservationDialog;
import org.helioviewer.jhv.gui.dialogs.SoarDialog;
import org.helioviewer.jhv.gui.dialogs.SynopticDialog;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.io.ExtensionFileFilter;
import org.helioviewer.jhv.io.Load;
import org.helioviewer.jhv.io.UpdateChecker;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.swing.TransferAccess;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.band.BandReaderHapi;

@SuppressWarnings({"serial", "this-escape"})
public class Actions {

    public static final AbstractAction PLAY_PAUSE = new PlayPauseAction();
    public static final AbstractAction PREVIOUS_FRAME = new PreviousFrameAction();
    public static final AbstractAction NEXT_FRAME = new NextFrameAction();

    public abstract static class AbstractKeyAction extends AbstractAction {
        public AbstractKeyAction(String name, KeyStroke key) {
            super(name);
            putValue(ACCELERATOR_KEY, key);
        }
    }

    public static class ClearAnnotations extends AbstractAction {
        public ClearAnnotations() {
            super("Clear Annotations");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Annotations.clear();
            MovieDisplay.display();
        }
    }

    public static class ExitProgram extends AbstractKeyAction {
        public ExitProgram() {
            super("Quit", KeyStroke.getKeyStroke(KeyEvent.VK_Q, UIGlobals.menuShortcutMask));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (ExitHooks.exitProgram())
                System.exit(0);
        }
    }

    public static class LoadState extends AbstractAction {
        public LoadState() {
            super("Load State...");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            File state = LoadStateDialog.get();
            if (state != null)
                Commands.loadState(state.toURI());
        }
    }

    public static class NewLayer extends AbstractKeyAction {
        public NewLayer() {
            super("New Image Layer...", KeyStroke.getKeyStroke(KeyEvent.VK_N, UIGlobals.menuShortcutMask));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ObservationDialog.getInstance().showDialog(true, null);
        }
    }

    public static class NewSoarLayer extends AbstractKeyAction {
        public NewSoarLayer() {
            super("New SOAR Layer...", KeyStroke.getKeyStroke(KeyEvent.VK_N, UIGlobals.menuShortcutMask | InputEvent.SHIFT_DOWN_MASK));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            SoarDialog.getInstance().showDialog();
        }
    }

    public static class NewSynopticLayer extends AbstractAction {
        public NewSynopticLayer() {
            super("New Synoptic Layer...");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            SynopticDialog.getInstance().showDialog();
        }
    }

    public static class OpenLocalFile extends AbstractKeyAction {
        public OpenLocalFile() {
            super("Open Image Layer...", KeyStroke.getKeyStroke(KeyEvent.VK_O, UIGlobals.menuShortcutMask));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            FileDialog fileDialog = new FileDialog(JHVFrame.getFrame(), "Choose a file", FileDialog.LOAD);
            // does not work on Windows
            fileDialog.setFilenameFilter(ExtensionFileFilter.Image);
            fileDialog.setMultipleMode(true);
            fileDialog.setDirectory(Settings.getProperty("path.local"));
            fileDialog.setVisible(true);

            String directory = fileDialog.getDirectory();
            File[] fileNames = fileDialog.getFiles();
            if (fileNames.length > 0 && directory != null) {
                Settings.setProperty("path.local", directory); // remember the current directory for future
                ArrayList<URI> uris = new ArrayList<>(fileNames.length);
                for (File f : fileNames) {
                    if (f.isFile() && f.canRead()) // cannot select directories anyway
                        uris.add(f.toURI());
                }
                Load.image(uris);
            }
        }
    }

    public static class OpenURLinBrowser extends AbstractAction {
        private final String urlToOpen;

        public OpenURLinBrowser(String name, String url) {
            super(name);
            urlToOpen = url;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JHVGlobals.openURL(urlToOpen);
        }
    }

    public static class Paste extends AbstractKeyAction {
        public Paste() {
            super("Paste", KeyStroke.getKeyStroke(KeyEvent.VK_V, UIGlobals.menuShortcutMask));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            TransferAccess.readClipboard();
        }
    }

    public static class ResetCamera extends AbstractAction {
        public ResetCamera() {
            super("Reset Camera");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Commands.resetView();
        }
    }

    public static class ReloadSources extends AbstractKeyAction {
        public ReloadSources() {
            super("Reload Datasets Listings", KeyStroke.getKeyStroke(KeyEvent.VK_R, UIGlobals.menuShortcutMask));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            DataSources.loadSources(false);
            BandReaderHapi.requestCatalog();
        }
    }

    public static class ResetCameraAxis extends AbstractAction {
        public ResetCameraAxis() {
            super("Reset Camera Axis");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Commands.resetViewAxis();
        }
    }

    public static class SeparateMultiviewZoom extends AbstractAction {
        public SeparateMultiviewZoom() {
            super("Separate Multiview Zoom");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Display.setSeparateViewportZoom(!Display.separateViewportZoom);
        }
    }

    public static class Rotate90Camera extends AbstractAction {
        private final String axis;

        public Rotate90Camera(String name, String _axis) {
            super(name);
            axis = _axis;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Commands.rotateView90(axis);
        }
    }

    private static class PlayPauseAction extends AbstractKeyAction {
        PlayPauseAction() {
            super("Play/Pause Movie", KeyStroke.getKeyStroke(KeyEvent.VK_P, UIGlobals.menuShortcutMask));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Commands.togglePlayback();
        }
    }

    private static class PreviousFrameAction extends AbstractKeyAction {
        PreviousFrameAction() {
            super("Step to Previous Frame", KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, UIGlobals.menuShortcutMask | InputEvent.ALT_DOWN_MASK));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (Movie.isPlaying())
                Commands.pause();
            Commands.previousFrame();
        }
    }

    private static class NextFrameAction extends AbstractKeyAction {
        NextFrameAction() {
            super("Step to Next Frame", KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, UIGlobals.menuShortcutMask | InputEvent.ALT_DOWN_MASK));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (Movie.isPlaying())
                Commands.pause();
            Commands.nextFrame();
        }
    }

    public static class SaveState extends AbstractKeyAction {
        public SaveState() {
            super("Save State", KeyStroke.getKeyStroke(KeyEvent.VK_S, UIGlobals.menuShortcutMask));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            State.save(Settings.getProperty("path.state"), "state__" + TimeUtils.formatFilename(System.currentTimeMillis()) + ".jhv");
        }
    }

    public static class SaveStateAs extends AbstractKeyAction {
        public SaveStateAs() {
            super("Save State As...", KeyStroke.getKeyStroke(KeyEvent.VK_S, UIGlobals.menuShortcutMask | InputEvent.ALT_DOWN_MASK));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            FileDialog fileDialog = new FileDialog(JHVFrame.getFrame(), "Save as...", FileDialog.SAVE);
            // does not work on Windows
            fileDialog.setFilenameFilter(ExtensionFileFilter.JHV);
            fileDialog.setMultipleMode(false);
            fileDialog.setDirectory(Settings.getProperty("path.state"));
            fileDialog.setFile("state__" + TimeUtils.formatFilename(System.currentTimeMillis()) + ".jhv");
            fileDialog.setVisible(true);

            String directory = fileDialog.getDirectory();
            String file = fileDialog.getFile();
            if (directory != null && file != null) {
                Settings.setProperty("path.state", directory); // remember the current directory for future
                if (!file.toLowerCase().endsWith(".jhv"))
                    file += ".jhv";
                State.save(directory, file);
            }
        }
    }

    public static class SDOCutOut extends AbstractAction {
        public SDOCutOut() {
            super("SDO Cut-out");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String baseURL = "https://www.lmsal.com/get_aia_data/?";
            JHVGlobals.openURL(baseURL + ImageLayers.getSDOCutoutString());
        }
    }

    public static class ShowDialog extends AbstractAction {
        private final Interfaces.ShowableDialog dialog;

        public ShowDialog(String name, Interfaces.ShowableDialog _dialog) {
            super(name);
            dialog = _dialog;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            dialog.showDialog();
        }
    }

    public static class CheckForUpdates extends AbstractAction {
        public CheckForUpdates() {
            super("Check for Updates...");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UpdateChecker.check(true);
        }
    }

    public static class WindowMinimize extends AbstractKeyAction {
        public WindowMinimize() {
            super("Minimize", KeyStroke.getKeyStroke(KeyEvent.VK_M, UIGlobals.menuShortcutMask));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int state = JHVFrame.getFrame().getExtendedState();
            state ^= JFrame.ICONIFIED;
            JHVFrame.getFrame().setExtendedState(state);
        }
    }

    public static class WindowZoom extends AbstractAction {
        public WindowZoom() {
            super("Zoom");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int state = JHVFrame.getFrame().getExtendedState();
            state ^= JFrame.MAXIMIZED_BOTH;
            JHVFrame.getFrame().setExtendedState(state);
        }
    }

    public static class ZoomFit extends AbstractKeyAction {
        public ZoomFit() {
            super("Zoom to Fit", KeyStroke.getKeyStroke(KeyEvent.VK_9, UIGlobals.menuShortcutMask));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Commands.zoomFit();
        }
    }

    public static class ZoomFOVAnnotation extends AbstractAction {
        public ZoomFOVAnnotation() {
            super("Fit FOV Annotation");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Annotations.zoom(Display.getCamera());
            MovieDisplay.render(1);
        }
    }

    public static class ZoomIn extends AbstractKeyAction {
        public ZoomIn() {
            super("Zoom In", KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, UIGlobals.menuShortcutMask));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Commands.zoomIn();
        }
    }

    public static class ZoomOneToOne extends AbstractKeyAction {
        public ZoomOneToOne() {
            super("Actual Size", KeyStroke.getKeyStroke(KeyEvent.VK_0, UIGlobals.menuShortcutMask));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Commands.zoomOneToOne();
        }
    }

    public static class ZoomOut extends AbstractKeyAction {
        public ZoomOut() {
            super("Zoom Out", KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, UIGlobals.menuShortcutMask));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Commands.zoomOut();
        }
    }

}
