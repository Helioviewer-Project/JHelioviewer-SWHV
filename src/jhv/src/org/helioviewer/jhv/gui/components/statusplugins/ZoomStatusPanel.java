package org.helioviewer.jhv.gui.components.statusplugins;

import org.helioviewer.jhv.gui.components.StatusPanel;

@SuppressWarnings("serial")
public class ZoomStatusPanel extends StatusPanel.StatusPlugin {

    public ZoomStatusPanel() {
        update(1);
    }

    public void update(double cameraWidth) {
        setText(String.format("FOV: %.2fR\u2299", 2 * cameraWidth));
    }

}
