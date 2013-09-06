package org.helioviewer.jhv.gui.controller;

import java.awt.event.MouseEvent;

import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.jhv.gui.components.BasicImagePanel;
import org.helioviewer.jhv.gui.interfaces.ImagePanelInputController;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.view.ViewportView;

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

    protected volatile View view;
    protected volatile RegionView regionView;
    protected volatile ViewportView viewportView;
    protected volatile MetaDataView metaDataView;
    protected volatile BasicImagePanel imagePanel;
    protected volatile Vector2dInt mousePosition = null;

    // ///////////////////////////////////////////////////////////////////////////
    // Methods
    // ///////////////////////////////////////////////////////////////////////////

    /**
     * Get the assigned image panel of this pan controller instance
     * 
     * @return Reference to the assigned image panel.
     * */
    public BasicImagePanel getImagePanel() {
        return imagePanel;
    }

    /**
     * Get the assigned view of this pan controller instance.
     * 
     * @return Reference to the assigned view.
     * */
    public View getView() {
        return view;
    }

    /**
     * Assigns a image panel to this pan controller instance.
     * 
     * @param newImagePanel
     *            Image panel
     * */
    public void setImagePanel(BasicImagePanel newImagePanel) {
        imagePanel = newImagePanel;
    }

    /**
     * Assigns a view to this pan controller instance.
     * 
     * @param newView
     *            View
     * */
    public void setView(View newView) {
        view = newView;
        regionView = ViewHelper.getViewAdapter(view, RegionView.class);
        viewportView = ViewHelper.getViewAdapter(view, ViewportView.class);
        metaDataView = ViewHelper.getViewAdapter(view, MetaDataView.class);
    }

    /**
     * {@inheritDoc}
     */
    public void detach() {
    }

    /**
     * Updates the mouse position {@inheritDoc}
     */
    public void mouseMoved(MouseEvent e) {
        mousePosition = new Vector2dInt(e.getX(), e.getY());
    }

    /**
     * Updates the mouse position {@inheritDoc}
     */
    public void mouseExited(MouseEvent e) {
        mousePosition = null;
    }

    /**
     * {@inheritDoc}
     */
    public Vector2dInt getMousePosition() {
        return mousePosition;
    }
}
