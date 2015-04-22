package org.helioviewer.jhv.gui.components.statusplugins;

import java.awt.Dimension;

import javax.swing.BorderFactory;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.viewmodel.view.AbstractView;

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
    private static final ZoomStatusPanel instance = new ZoomStatusPanel();

    private ZoomStatusPanel() {
        setBorder(BorderFactory.createEtchedBorder());
        setPreferredSize(new Dimension(100, 20));
        setText("Zoom:");
    }

    public static ZoomStatusPanel getSingletonInstance() {
        return instance;
    }

    /**
     * Updates the displayed zoom.
     */
    private void updateZoomLevel(AbstractView view) {
        if (view != null) {
            // TBD
            setVisible(true);
        } else {
            setVisible(false);
        }
    }

    public void updateZoomLevel() {
        updateZoomLevel(Displayer.getLayersModel().getActiveView());
    }

}
