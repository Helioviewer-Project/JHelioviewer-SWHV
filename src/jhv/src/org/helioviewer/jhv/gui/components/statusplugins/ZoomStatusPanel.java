package org.helioviewer.jhv.gui.components.statusplugins;

import java.awt.Dimension;

import javax.swing.BorderFactory;

import org.helioviewer.jhv.gui.controller.ZoomController;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.View;

/**
 * Status panel for displaying the current zoom.
 * 
 * <p>
 * A displayed zoom of 100% means that one pixel one the screen corresponds to
 * exactly one pixel in the native resolution of the image.
 * 
 * <p>
 * The information of this panel is always shown for the active layer.
 * 
 * <p>
 * If there is no layer present, this panel will be invisible.
 */
public class ZoomStatusPanel extends ViewStatusPanelPlugin {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public ZoomStatusPanel() {
        setBorder(BorderFactory.createEtchedBorder());

        setPreferredSize(new Dimension(100, 20));
        setText("Zoom:");

        LayersModel.getSingletonInstance().addLayersListener(this);
    }

    /**
     * Updates the displayed zoom.
     */
    private synchronized void updateZoomLevel() {
        View view = LayersModel.getSingletonInstance().getActiveView();

        if (view != null) {
            long zoom = Math.round(ZoomController.getZoom(view) * 100);
            if (zoom != 0.0) {
                setText("Zoom: " + zoom + "%");
            } else {
                setText("Zoom: n/a");
            }
            setVisible(true);
        } else {
            setVisible(false);
        }
    }

    public void activeLayerChanged(int idx) {
        updateZoomLevel();
    }

    public void viewportGeometryChanged() {
        updateZoomLevel();
    }

}
