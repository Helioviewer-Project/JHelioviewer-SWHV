package org.helioviewer.jhv;

import org.helioviewer.jhv.export.ExportMovie;

public class ExitHooks {

    private static final Thread finishMovieThread = new Thread(() -> {
        try {
            ExportMovie.disposeMovieWriter(false);
        } catch (Exception e) {
            Log.warn("Movie was not shut down properly");
        }
    });

    public static void attach() {
        // At the moment this runs, the EventQueue is blocked (by enforcing to run System.exit on it which is blocking)
        Runtime.getRuntime().addShutdownHook(finishMovieThread);
    }

    public static boolean exitProgram() {
        return true;
    }

}
