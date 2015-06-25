package org.helioviewer.jhv.gui.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.actions.filefilters.ExtensionFileFilter;
import org.helioviewer.jhv.gui.actions.filefilters.JPGFilter;
import org.helioviewer.jhv.gui.actions.filefilters.PNGFilter;

/**
 * Action to save a screenshot in desired image format at desired location
 * Therefore opens a save dialog to choose format, name and location
 * 
 * @author Markus Langenberg
 */
@SuppressWarnings("serial")
public class SaveScreenshotAsAction extends AbstractAction {

    public SaveScreenshotAsAction() {
        super("Save screenshot as...");
        putValue(SHORT_DESCRIPTION, "Save screenshot to chosen folder");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.SHIFT_DOWN_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.addChoosableFileFilter(new JPGFilter());
        fileChooser.addChoosableFileFilter(new PNGFilter());

        fileChooser.setSelectedFile(new File(JHVDirectory.EXPORTS.getPath() + SaveScreenshotAction.getDefaultFileName()));
        int retVal = fileChooser.showSaveDialog(ImageViewerGui.getMainFrame());

        if (retVal == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            ExtensionFileFilter fileFilter = (ExtensionFileFilter) fileChooser.getFileFilter();

            if (!fileFilter.accept(selectedFile)) {
                selectedFile = new File(selectedFile.getPath() + "." + fileFilter.getDefaultExtension());
            }
            ImageViewerGui.getMainComponent().saveScreenshot(fileFilter.getDefaultExtension(), selectedFile);
        }
    }

}
