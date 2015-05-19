package org.helioviewer.jhv.gui.components.statusplugins;

import javax.swing.JLabel;

@SuppressWarnings({"serial"})
public class ZoomStatusPanel extends JLabel {

    public ZoomStatusPanel() {
        setText("Zoom:");
    }

    public void updateZoomLevel(double cameraWidth) {
        setText(String.format("Zoom: %.2f R\u2299", cameraWidth));
    }

}
