package org.helioviewer.jhv.gui.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.viewmodel.view.LayeredView;

/**
 * Action to terminate the application.
 * 
 * @author Markus Langenberg
 */
public class ExitProgramAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public ExitProgramAction() {
        super("Quit");
        putValue(SHORT_DESCRIPTION, "Quit program");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {

        if (ImageViewerGui.getSingletonInstance().getMainView() != null) {
            if (ImageViewerGui.getSingletonInstance().getMainView().getAdapter(LayeredView.class).getNumberOfVisibleLayer() > 0) {
                int option = JOptionPane.showConfirmDialog(ImageViewerGui.getMainFrame(), "Are you sure you want to quit?", "Confirm", JOptionPane.OK_CANCEL_OPTION);
                if (option == JOptionPane.CANCEL_OPTION) {
                    return;
                }
            }
        }

        // Delete all layers, to free resources
        if (ImageViewerGui.getSingletonInstance().getMainView() != null) {
            LayeredView layeredView = ImageViewerGui.getSingletonInstance().getMainView().getAdapter(LayeredView.class);

            while (layeredView.getNumLayers() > 0) {
                layeredView.removeLayer(0);
            }
        }

        // Delete all files in JHV/temp
        File[] tempFiles = JHVDirectory.TEMP.getFile().listFiles();

        for (File tempFile : tempFiles) {
            tempFile.delete();
        }

        System.exit(0);
    }

}
