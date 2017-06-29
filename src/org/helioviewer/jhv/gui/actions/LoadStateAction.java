package org.helioviewer.jhv.gui.actions;

import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.actions.filefilters.JsonFilenameFilter;

@SuppressWarnings("serial")
public class LoadStateAction extends AbstractAction {

    public LoadStateAction() {
        super("Load State...");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        FileDialog fileDialog = new FileDialog(ImageViewerGui.getMainFrame(), "Choose a file", FileDialog.LOAD);
        // does not work on Windows
        fileDialog.setFilenameFilter(new JsonFilenameFilter());
        fileDialog.setDirectory(JHVDirectory.STATES.getPath());
        fileDialog.setVisible(true);

        File[] fileNames = fileDialog.getFiles();
        if (fileNames.length > 0) {
            ImageViewerGui.getRenderableContainer().loadScene(fileNames[0].getPath());
        }
    }

}
