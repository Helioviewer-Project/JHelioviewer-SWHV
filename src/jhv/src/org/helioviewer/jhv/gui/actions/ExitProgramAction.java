package org.helioviewer.jhv.gui.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;

/**
 * Action to terminate the application.
 *
 * @author Markus Langenberg
 */
public class ExitProgramAction extends AbstractAction {

    public ExitProgramAction() {
        super("Quit");
        putValue(SHORT_DESCRIPTION, "Quit program");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (Displayer.getLayersModel().getNumLayers() > 0) {
            int option = JOptionPane.showConfirmDialog(ImageViewerGui.getMainFrame(), "Are you sure you want to quit?", "Confirm", JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.CANCEL_OPTION) {
                return;
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
        } catch (InterruptedException e1) {
            Log.warn("FileDelete job was interrupted");
        } catch (ExecutionException e2) {
            Log.warn("Caught exception on FileDelete: " + e);
        } catch (TimeoutException e3) {
            futureFileDelete.cancel(true);
            Log.warn("Timeout upon deleting temporary files");
        }

        System.exit(0);
    }

}
