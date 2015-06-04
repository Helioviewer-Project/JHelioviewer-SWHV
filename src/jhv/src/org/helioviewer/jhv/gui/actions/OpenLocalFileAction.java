package org.helioviewer.jhv.gui.actions;

import java.awt.Frame;
import java.awt.FileDialog;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.actions.filefilters.AllSupportedImageTypesFilenameFilter;
import org.helioviewer.jhv.io.APIRequestManager;
import org.helioviewer.jhv.layers.LayersModel;

/**
 * Action to open a local file.
 * <p>
 * Opens a file chooser dialog, opens the selected file. Currently supports the
 * following file extensions: "jpg", "jpeg", "png", "fts", "fits", "jp2" and
 * "jpx"
 */
@SuppressWarnings({"serial"})
public class OpenLocalFileAction extends AbstractAction {

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
        FileDialog fileDialog = new FileDialog(ImageViewerGui.getMainFrame(), "Choose a file", FileDialog.LOAD);
        // does not work on Windows
        fileDialog.setFilenameFilter(new AllSupportedImageTypesFilenameFilter());
        fileDialog.setDirectory(Settings.getSingletonInstance().getProperty("default.local.path"));
        fileDialog.setVisible(true);

        String directory = fileDialog.getDirectory();
        String fileName = fileDialog.getFile();

        if (fileName != null && directory != null) {
            File selectedFile = new File(directory + File.separator + fileName);

            if (selectedFile.exists() && selectedFile.isFile()) {
                // remember the current directory for future
                Settings.getSingletonInstance().setProperty("default.local.path", directory);
                Settings.getSingletonInstance().save();

                final URI uri = selectedFile.toURI();
                // Load image in new thread
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            LayersModel.addLayerFromThread(APIRequestManager.loadView(uri, uri));
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
