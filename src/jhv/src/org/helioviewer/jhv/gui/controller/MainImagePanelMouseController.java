package org.helioviewer.jhv.gui.controller;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.states.StateController;
import org.helioviewer.viewmodel.view.View;

/**
 * Abstract base class implementing ImagePanelInputController for the main image
 * panel.
 *
 * <p>
 * This class implements the capability to zoom within the main image panel by
 * using the mouse wheel or by double-clicking. Since this functionality is
 * common to all ImagePanelInputControllers used for the main image panel, there
 * centralized here.
 *
 * <p>
 * Also see {@link org.helioviewer.jhv.gui.components.MainImagePanel}
 *
 * @author Markus Langenberg
 *
 */
public abstract class MainImagePanelMouseController extends AbstractImagePanelMouseController {

    // ///////////////////////////////////////////////////////////////////////////
    // Definitions
    // ///////////////////////////////////////////////////////////////////////////

    private volatile ZoomController zoomController = new ZoomController();

    // ///////////////////////////////////////////////////////////////////////////
    // Methods
    // ///////////////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    public void setView(View newView) {

        super.setView(newView);

        zoomController.setImagePanel(ImageViewerGui.getSingletonInstance().getMainImagePanel());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() > 1) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                zoomController.zoomSteps(getView(), 2);
            } else if (e.getButton() == MouseEvent.BUTTON3) {
                zoomController.zoomSteps(getView(), -2);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (zoomController != null) {
            zoomController.zoomSteps(getView(), (int) (-Math.max(1, e.getUnitsToScroll() / 3) * Math.signum(e.getUnitsToScroll())));
            double zoom = ZoomController.getZoom(getView());

            if (zoom < 1 / (Constants.SunRadius * 1.5)) {
                StateController.getInstance().set3DState();
            }

        }
    }
}