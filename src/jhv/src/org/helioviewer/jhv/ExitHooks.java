package org.helioviewer.jhv;

import java.io.File;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.JOptionPane;

import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.export.MovieExporter;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;

public class ExitHooks {

    static Thread finishMovieThread = new Thread() {
        @Override
        public void run() {
            try {
                MovieExporter.disposeMovieWriter(false);
            } catch (Exception e) {
                Log.warn("Movie was not shut down properly");
            }
        }
    };

    public static void attach() {
        // At the moment this runs, the EventQueue is blocked (by enforcing to run System.exit on it which is blocking)
        Runtime.getRuntime().addShutdownHook(finishMovieThread);
    }

    public static boolean exitProgram() {
        if (Layers.getNumLayers() > 0) {
            int option = JOptionPane.showConfirmDialog(ImageViewerGui.getMainFrame(), "Are you sure you want to quit?", "Confirm", JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.CANCEL_OPTION) {
                return false;
            }
        }

        final ExecutorService executor = Executors.newFixedThreadPool(4);
        Future<?> futureFileDelete = executor.submit(new Runnable() {
            @Override
            public void run() {
                File[] tempFiles = JHVDirectory.TEMP.getFile().listFiles();

                for (File tempFile : tempFiles) {
                    tempFile.delete();
                }
            }
        });
        executor.shutdown();

        try {
            futureFileDelete.get(1500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Log.warn("FileDelete job was interrupted");
        } catch (ExecutionException e) {
            Log.warn("Caught exception on FileDelete: " + e);
        } catch (TimeoutException e) {
            futureFileDelete.cancel(true);
            Log.warn("Timeout upon deleting temporary files");
        }

        System.exit(0);
        return true;
    }

}
