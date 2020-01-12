package org.helioviewer.jhv.gui.actions;

import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.io.ExtensionFileFilter;
import org.helioviewer.jhv.io.Load;

@SuppressWarnings("serial")
public class LoadStateAction extends AbstractAction {

    public LoadStateAction() {
        super("Load State...");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        FileDialog fileDialog = new FileDialog(JHVFrame.getFrame(), "Choose a file", FileDialog.LOAD);
        // does not work on Windows
        fileDialog.setFilenameFilter(ExtensionFileFilter.JHV);
        fileDialog.setDirectory(JHVDirectory.STATES.getPath());
        fileDialog.setVisible(true);

        File[] fileNames = fileDialog.getFiles();
        if (fileNames.length > 0 && fileNames[0].isFile())
            Load.state.get(fileNames[0].toURI());
    }

}
