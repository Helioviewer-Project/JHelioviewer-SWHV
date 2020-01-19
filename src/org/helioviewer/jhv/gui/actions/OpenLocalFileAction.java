package org.helioviewer.jhv.gui.actions;

import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.input.KeyShortcuts;
import org.helioviewer.jhv.io.ExtensionFileFilter;
import org.helioviewer.jhv.io.Load;

@SuppressWarnings("serial")
public class OpenLocalFileAction extends AbstractAction {

    public OpenLocalFileAction() {
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
