package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.controller.ZoomController;

/**
 * Action to zoom in.
 *
 * @author Markus Langenberg
 */
public class ZoomInAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private ZoomController zoomController;

    /**
     * Constructor
     *
     * @param small
     *            - if true, chooses a small (16x16), otherwise a large (24x24)
     *            icon for the action
     */
    public ZoomInAction(boolean small) {
        super("Zoom in", small ? IconBank.getIcon(JHVIcon.ZOOM_IN_SMALL) : IconBank.getIcon(JHVIcon.ZOOM_IN));
        putValue(SHORT_DESCRIPTION, "Zoom in x2");
        putValue(MNEMONIC_KEY, KeyEvent.VK_I);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.ALT_MASK));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent arg0) {
        if (zoomController == null) {
            zoomController = new ZoomController();
            zoomController.setImagePanel(ImageViewerGui.getSingletonInstance().getMainImagePanel());
        }
        zoomController.zoomSteps(Displayer.getLayersModel().getActiveView(), 2);
    }

}