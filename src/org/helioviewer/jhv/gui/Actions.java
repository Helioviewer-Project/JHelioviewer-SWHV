package org.helioviewer.jhv.gui;

import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.ExitHooks;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.gui.dialogs.LoadStateDialog;
import org.helioviewer.jhv.gui.dialogs.ObservationDialog;
import org.helioviewer.jhv.gui.dialogs.SoarDialog;
import org.helioviewer.jhv.input.KeyShortcuts;
import org.helioviewer.jhv.io.ExtensionFileFilter;
import org.helioviewer.jhv.io.Load;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.layers.selector.State;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.time.TimeUtils;

@SuppressWarnings("serial")
public class Actions {

    public static class ClearAnnotations extends AbstractAction {

        public ClearAnnotations() {
            super("Clear Annotations");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JHVFrame.getInteraction().clearAnnotations();
            MovieDisplay.display();
        }

    }

    public static final class ExitProgram extends AbstractAction {

        public ExitProgram() {
            super("Quit");

            KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_Q, UIGlobals.menuShortcutMask);
            putValue(ACCELERATOR_KEY, key);
            KeyShortcuts.registerKey(key, this);
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
                Load.state.get(state.toURI());
        }

    }

    public static final class NewLayer extends AbstractAction {

        public NewLayer() {
            super("New Image Layer...");

            KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_N, UIGlobals.menuShortcutMask);
            putValue(ACCELERATOR_KEY, key);
            KeyShortcuts.registerKey(key, this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ObservationDialog.getInstance().showDialog(true, null);
        }

    }

    public static final class NewSoarLayer extends AbstractAction {

        public NewSoarLayer() {
            super("New SOAR Layer...");

            KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_N, UIGlobals.menuShortcutMask | InputEvent.SHIFT_DOWN_MASK);
            putValue(ACCELERATOR_KEY, key);
            KeyShortcuts.registerKey(key, this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            SoarDialog.getInstance().showDialog();
        }

    }

    public static final class OpenLocalFile extends AbstractAction {

        public OpenLocalFile() {
            super("Open Image Layer...");

            KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_O, UIGlobals.menuShortcutMask);
            putValue(ACCELERATOR_KEY, key);
            KeyShortcuts.registerKey(key, this);
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
                Load.Image.getAll(uris);
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

    public static final class Paste extends AbstractAction {

        public Paste() {
            super("Paste");

            KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_V, UIGlobals.menuShortcutMask);
            putValue(ACCELERATOR_KEY, key);
            KeyShortcuts.registerKey(key, this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JHVTransferHandler.getInstance().readClipboard();
        }

    }

    public static class ResetCamera extends AbstractAction {

        public ResetCamera() {
            super("Reset Camera");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Display.getCamera().reset();
        }

    }

    public static class ResetCameraAxis extends AbstractAction {

        public ResetCameraAxis() {
            super("Reset Camera Axis");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Display.getCamera().resetDragRotationAxis();
            MovieDisplay.display();
        }

    }

    public static class Rotate90Camera extends AbstractAction {

        private final Quat rotation;

        public Rotate90Camera(String name, Vec3 axis) {
            super(name);
            rotation = Quat.createRotation(Math.PI / 2, axis);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Display.getCamera().rotateDragRotation(rotation);
            MovieDisplay.display();
        }

    }

    public static final class SaveState extends AbstractAction {

        public SaveState() {
            super("Save State");

            KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_S, UIGlobals.menuShortcutMask);
            putValue(ACCELERATOR_KEY, key);
            KeyShortcuts.registerKey(key, this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            State.save(Settings.getProperty("path.state"), "state__" + TimeUtils.formatFilename(System.currentTimeMillis()) + ".jhv");
        }

    }

    public static final class SaveStateAs extends AbstractAction {

        public SaveStateAs() {
            super("Save State As...");

            KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_S, UIGlobals.menuShortcutMask | InputEvent.ALT_DOWN_MASK);
            putValue(ACCELERATOR_KEY, key);
            KeyShortcuts.registerKey(key, this);
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
                if (!file.toLowerCase(Locale.ENGLISH).endsWith(".jhv"))
                    file += ".jhv";
                State.save(directory, file);
            }
        }

    }

    public static class SDOCutOut extends AbstractAction {

        private static final String baseURL = "https://www.lmsal.com/get_aia_data/?";

        public SDOCutOut() {
            super("SDO Cut-out");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
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

    public static class UpdateChecker extends AbstractAction {

        public UpdateChecker() {
            super("Check for Updates...");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            org.helioviewer.jhv.io.UpdateChecker.check(true);
        }

    }

    public static final class WindowMinimize extends AbstractAction {

        public WindowMinimize() {
            super("Minimize");

            KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_M, UIGlobals.menuShortcutMask);
            putValue(ACCELERATOR_KEY, key);
            KeyShortcuts.registerKey(key, this);
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

    public static final class ZoomFit extends AbstractAction {

        public ZoomFit() {
            super("Zoom to Fit");

            KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_9, UIGlobals.menuShortcutMask);
            putValue(ACCELERATOR_KEY, key);
            KeyShortcuts.registerKey(key, this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            CameraHelper.zoomToFit(Display.getCamera());
            MovieDisplay.render(1);
        }

    }

    public static class ZoomFOVAnnotation extends AbstractAction {

        public ZoomFOVAnnotation() {
            super("Fit FOV Annotation");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JHVFrame.getInteraction().zoomAnnotations();
            MovieDisplay.render(1);
        }

    }

    public static final class ZoomIn extends AbstractAction {

        public ZoomIn() {
            super("Zoom In");

            KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, UIGlobals.menuShortcutMask);
            putValue(ACCELERATOR_KEY, key);
            KeyShortcuts.registerKey(key, this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Display.getCamera().zoom(-Camera.ZOOM_MULTIPLIER_BUTTON);
            MovieDisplay.render(1);
        }

    }

    public static final class ZoomOneToOne extends AbstractAction {

        public ZoomOneToOne() {
            super("Actual Size");

            KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_0, UIGlobals.menuShortcutMask);
            putValue(ACCELERATOR_KEY, key);
            KeyShortcuts.registerKey(key, this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ImageLayer layer = Layers.getActiveImageLayer();
            if (layer != null) {
                MetaData m = layer.getMetaData();
                Camera camera = Display.getCamera();
                double imageFraction = Display.getActiveViewport().height / (double) m.getPixelHeight();
                double fov = 2. * Math.atan2(0.5 * m.getPhysicalRegion().height * imageFraction, camera.getViewpoint().distance);
                camera.setFOV(fov);

                MovieDisplay.render(1);
            }
        }

    }

    public static final class ZoomOut extends AbstractAction {

        public ZoomOut() {
            super("Zoom Out");

            KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, UIGlobals.menuShortcutMask);
            putValue(ACCELERATOR_KEY, key);
            KeyShortcuts.registerKey(key, this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Display.getCamera().zoom(+Camera.ZOOM_MULTIPLIER_BUTTON);
            MovieDisplay.display();
        }

    }

}
