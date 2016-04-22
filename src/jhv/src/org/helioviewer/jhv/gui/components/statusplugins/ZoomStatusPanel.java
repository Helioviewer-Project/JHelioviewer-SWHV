package org.helioviewer.jhv.gui.components.statusplugins;

import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.gui.components.StatusPanel;

@SuppressWarnings("serial")
public class ZoomStatusPanel extends StatusPanel.StatusPlugin {

    public ZoomStatusPanel() {
        update(1, 1);
    }

    public void update(double cameraWidth, double distance) {
        setText(String.format("FOV: %5.2fR\u2299 | D\u2299: %6.3fau", 2 * cameraWidth, distance / Sun.MeanEarthDistance));
    }

}
