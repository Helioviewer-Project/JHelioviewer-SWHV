package org.helioviewer.jhv.gui.components.statusplugins;

import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.gui.components.StatusPanel;

@SuppressWarnings("serial")
public class ZoomStatusPanel extends StatusPanel.StatusPlugin {

    public ZoomStatusPanel() {
        update(1, 1);
    }

    String formatFOV(double r) {
        if (r < 2 * 32 * Sun.Radius)
            return String.format("%5.2fR\u2299", r);
        else
            return String.format("%.2fau", r * Sun.MeanEarthDistanceInv);
    }

    public void update(double cameraWidth, double distance) {
        setText(String.format("FOV: %s | D\u2299: %6.3fau", formatFOV(2 * cameraWidth), distance * Sun.MeanEarthDistanceInv));
    }

}
