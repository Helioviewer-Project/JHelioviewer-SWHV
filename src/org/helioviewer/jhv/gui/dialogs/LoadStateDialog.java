package org.helioviewer.jhv.gui.dialogs;

import java.awt.FileDialog;
import java.io.File;

import javax.annotation.Nullable;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.io.ExtensionFileFilter;

public class LoadStateDialog {

    @Nullable
    public static File get() {
        FileDialog fileDialog = new FileDialog(JHVFrame.getFrame(), "Choose a file", FileDialog.LOAD);
        // does not work on Windows
        fileDialog.setFilenameFilter(ExtensionFileFilter.JHV);
        fileDialog.setDirectory(JHVDirectory.STATES.getPath());
        fileDialog.setVisible(true);

        File[] fileNames = fileDialog.getFiles();
        return fileNames.length > 0 && fileNames[0].isFile() ? fileNames[0] : null;
    }

}
