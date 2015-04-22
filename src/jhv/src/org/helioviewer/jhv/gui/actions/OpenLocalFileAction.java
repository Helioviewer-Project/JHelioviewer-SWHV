package org.helioviewer.jhv.gui.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.KeyStroke;

import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.actions.filefilters.AllSupportedImageTypesFilter;
import org.helioviewer.jhv.gui.actions.filefilters.FitsFilter;
import org.helioviewer.jhv.gui.actions.filefilters.JP2Filter;
import org.helioviewer.jhv.gui.actions.filefilters.JPGFilter;
import org.helioviewer.jhv.gui.actions.filefilters.PNGFilter;
import org.helioviewer.jhv.io.APIRequestManager;

/**
 * Action to open a local file.
 *
 * <p>
 * Opens a file chooser dialog, opens the selected file. Currently supports the
 * following file extensions: "jpg", "jpeg", "png", "fts", "fits", "jp2" and
 * "jpx"
 *
 * @author Markus Langenberg
 */
public class OpenLocalFileAction extends AbstractAction {

    /**
     * Default constructor.
     */
    public OpenLocalFileAction() {
        super("Open...");
        putValue(SHORT_DESCRIPTION, "Open new image");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent e) {

        final JFileChooser fileChooser = new JFileChooser(Settings.getSingletonInstance().getProperty("default.local.path"));
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.addChoosableFileFilter(new JP2Filter());
        fileChooser.addChoosableFileFilter(new FitsFilter());
        fileChooser.addChoosableFileFilter(new PNGFilter());
        fileChooser.addChoosableFileFilter(new JPGFilter());
        fileChooser.setFileFilter(new AllSupportedImageTypesFilter());
        fileChooser.setMultiSelectionEnabled(false);

        int retVal = fileChooser.showOpenDialog(ImageViewerGui.getMainFrame());

        if (retVal == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            if (selectedFile.exists() && selectedFile.isFile()) {
                // remember the current directory for future
                Settings.getSingletonInstance().setProperty("default.local.path", fileChooser.getSelectedFile().getParent());
                Settings.getSingletonInstance().save();

                // Load image in new thread
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            APIRequestManager.newLoad(fileChooser.getSelectedFile().toURI(), true);
                        } catch (IOException e) {
                            Message.err("An error occured while opening the file!", e.getMessage(), false);
                        }
                    }
                }, "OpenLocalFile");
                thread.start();
            }
        }
    }

}
