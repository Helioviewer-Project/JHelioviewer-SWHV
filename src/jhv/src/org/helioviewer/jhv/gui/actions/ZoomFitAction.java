package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.controller.ZoomController;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.RegionView;

/**
 * Action to zoom, such that the active layer fits completely in the viewport.
 * 
 * @author Markus Langenberg
 */
public class ZoomFitAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    ZoomController zoomController;

    /**
     * Constructor
     * 
     * @param small
     *            - if true, chooses a small (16x16), otherwise a large (24x24)
     *            icon for the action
     */
    public ZoomFitAction(boolean small) {
        super("Zoom to Fit", small ? IconBank.getIcon(JHVIcon.ZOOM_FIT_SMALL) : IconBank.getIcon(JHVIcon.ZOOM_FIT));
        putValue(SHORT_DESCRIPTION, "Zoom to Fit");
        putValue(MNEMONIC_KEY, KeyEvent.VK_F);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_K, KeyEvent.ALT_MASK));
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent arg0) {
        if (zoomController == null) {
            zoomController = new ZoomController();
        }
        zoomController.zoomFit(LayersModel.getSingletonInstance().getActiveView().getAdapter(MetaDataView.class), ImageViewerGui.getSingletonInstance().getMainView().getAdapter(RegionView.class));
    }

}