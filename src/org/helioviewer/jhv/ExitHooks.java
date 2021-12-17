package org.helioviewer.jhv;

import org.helioviewer.jhv.export.ExportMovie;

import java.lang.invoke.MethodHandles;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExitHooks {

    private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    private static final Thread finishMovieThread = new Thread(() -> {
        try {
            ExportMovie.disposeMovieWriter(false);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Movie was not shut down properly");
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
