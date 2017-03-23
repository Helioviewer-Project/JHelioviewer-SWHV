package org.helioviewer.jhv;

import javax.swing.JOptionPane;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.export.ExportMovie;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;

public class ExitHooks {

    private static final Thread finishMovieThread = new Thread(() -> {
        try {
            ExportMovie.getInstance().disposeMovieWriter(false);
        } catch (Exception e) {
            Log.warn("Movie was not shut down properly");
        }
    });

    public static void attach() {
        // At the moment this runs, the EventQueue is blocked (by enforcing to run System.exit on it which is blocking)
        Runtime.getRuntime().addShutdownHook(finishMovieThread);
    }

    public static boolean exitProgram() {
        return !(Layers.getNumLayers() > 0 &&
                JOptionPane.showConfirmDialog(ImageViewerGui.getMainFrame(), "Are you sure you want to quit?", "Confirm", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION);
    }

}
