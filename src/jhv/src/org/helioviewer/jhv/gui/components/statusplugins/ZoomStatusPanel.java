package org.helioviewer.jhv.gui.components.statusplugins;

import javax.swing.JLabel;

import org.helioviewer.jhv.gui.components.StatusPanel;

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
public class ZoomStatusPanel extends JLabel {

    private static final ZoomStatusPanel instance = new ZoomStatusPanel();

    private ZoomStatusPanel() {
        setBorder(StatusPanel.paddingBorder);

        getPreferredSize().height = StatusPanel.HEIGHT;
        //setPreferredSize(new Dimension(100, 20));
        setText("Zoom:");
    }

    public static ZoomStatusPanel getSingletonInstance() {
        return instance;
    }

    /**
     * Updates the displayed zoom.
     */
    public void updateZoomLevel(double cameraWidth) {
        setText(String.format("Zoom: %.2f Rsun", cameraWidth));
    }
}
