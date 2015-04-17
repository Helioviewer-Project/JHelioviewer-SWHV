package org.helioviewer.jhv.gui.controller;

import java.awt.event.MouseEvent;

import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.jhv.gui.components.MainImagePanel;
import org.helioviewer.jhv.gui.interfaces.ImagePanelInputController;
import org.helioviewer.viewmodel.view.ComponentView;

/**
 * Abstract base class implementing ImagePanelInputController.
 *
 * <p>
 * This class provides some very basic function for every input input
 * controller. It covers all functions dealing with managing the associated view
 * and image panel.
 *
 * @author Stephan Pagel
 *
 * */
public abstract class AbstractImagePanelMouseController implements ImagePanelInputController {

    // ///////////////////////////////////////////////////////////////////////////
    // Class variables
    // ///////////////////////////////////////////////////////////////////////////

    protected ComponentView view;
    protected MainImagePanel imagePanel;
    protected Vector2dInt mousePosition = null;

    // ///////////////////////////////////////////////////////////////////////////
    // Methods
    // ///////////////////////////////////////////////////////////////////////////

    /**
     * Get the assigned image panel of this pan controller instance
     *
     * @return Reference to the assigned image panel.
     * */
    @Override
    public MainImagePanel getImagePanel() {
        return imagePanel;
    }

    @Override
    public ComponentView getView() {
        return view;
    }

    /**
     * Assigns a image panel to this pan controller instance.
     *
     * @param newImagePanel
     *            Image panel
     * */
    @Override
    public void setImagePanel(MainImagePanel newImagePanel) {
        imagePanel = newImagePanel;
    }

    /**
     * Assigns a view to this pan controller instance.
     *
     * @param newView
     *            View
     * */
    @Override
    public void setView(ComponentView newView) {
        view = newView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void detach() {
    }

    /**
     * Updates the mouse position {@inheritDoc}
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        mousePosition = new Vector2dInt(e.getX(), e.getY());
    }

    /**
     * Updates the mouse position {@inheritDoc}
     */
    @Override
    public void mouseExited(MouseEvent e) {
        mousePosition = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector2dInt getMousePosition() {
        return mousePosition;
    }

}
