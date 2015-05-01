package org.helioviewer.jhv.gui.components.statusplugins;

import javax.swing.JLabel;

public class ZoomStatusPanel extends JLabel {

    private static final ZoomStatusPanel instance = new ZoomStatusPanel();

    private ZoomStatusPanel() {
        setText("Zoom:");
    }

    public static ZoomStatusPanel getSingletonInstance() {
        return instance;
    }

    /**
     * Updates the displayed zoom.
     */
    public void updateZoomLevel(double cameraWidth) {
        setText(String.format("Zoom: %.2f R\u2299", cameraWidth));
    }

}
