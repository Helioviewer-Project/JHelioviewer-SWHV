package org.helioviewer.jhv.gui.components.statusplugins;

import org.helioviewer.jhv.gui.components.StatusPanel;

@SuppressWarnings("serial")
public class ZoomStatusPanel extends StatusPanel.StatusPlugin {

    public ZoomStatusPanel() {
        updateZoomLevel(1);
    }

    public void updateZoomLevel(double cameraWidth) {
        setText(String.format("Zoom: %.2f R\u2299", 2 * cameraWidth));
    }

}
