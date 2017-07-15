package org.helioviewer.jhv.gui.actions;

import java.awt.FileDialog;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.actions.filefilters.AllFilenameFilter;
import org.helioviewer.jhv.input.KeyShortcuts;
import org.helioviewer.jhv.io.Load;

/**
 * Action to open a local file
 * Opens a file chooser dialog, opens the selected file. Currently supports the
 * following file extensions: "jpg", "jpeg", "png", "fts", "fits", "jp2" and
 * "jpx"
 */
@SuppressWarnings("serial")
public class OpenLocalFileAction extends AbstractAction {

    public OpenLocalFileAction() {
        super("Open Image Layer...");

        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        putValue(ACCELERATOR_KEY, key);
        KeyShortcuts.registerKey(key, this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        FileDialog fileDialog = new FileDialog(ImageViewerGui.getMainFrame(), "Choose a file", FileDialog.LOAD);
        // does not work on Windows
        fileDialog.setFilenameFilter(new AllFilenameFilter());
        fileDialog.setMultipleMode(true);
        fileDialog.setDirectory(Settings.getSingletonInstance().getProperty("default.local.path"));
        fileDialog.setVisible(true);

        String directory = fileDialog.getDirectory();
        File[] fileNames = fileDialog.getFiles();
        if (fileNames.length > 0 && directory != null) {
            // remember the current directory for future
            Settings.getSingletonInstance().setProperty("default.local.path", directory);
            Settings.getSingletonInstance().save("default.local.path");
            for (File fileName : fileNames) {
                if (fileName.isFile())
                    Load.image.get(fileName.toURI());
            }
        }
    }

}
