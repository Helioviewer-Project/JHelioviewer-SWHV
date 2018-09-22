package org.helioviewer.jhv.gui.components.statusplugin;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.gui.components.StatusPanel;
import org.helioviewer.jhv.gui.interfaces.LazyComponent;

@SuppressWarnings("serial")
public class ZoomStatusPanel extends StatusPanel.StatusPlugin implements LazyComponent {

    private boolean dirty;
    private double cameraWidth;
    private double distance;

    public ZoomStatusPanel() {
        update(Display.getCamera().getWidth(), Display.getCamera().getViewpoint().distance);
        UITimer.register(this);
    }

    private static String formatFOV(double r) {
        if (r < 2 * 32 * Sun.Radius)
            return String.format("%6.2fR\u2299", r);
        else
            return String.format("%6.2fau", r * Sun.MeanEarthDistanceInv);
    }

    public void update(double _cameraWidth, double _distance) {
        cameraWidth = _cameraWidth;
        distance = _distance;
        dirty = true;
    }

    @Override
    public void lazyRepaint() {
        if (dirty) {
            setText(String.format("FOV: %s | D\u2299: %7.3fau", formatFOV(2 * cameraWidth), distance * Sun.MeanEarthDistanceInv));
            dirty = false;
        }
    }

}
