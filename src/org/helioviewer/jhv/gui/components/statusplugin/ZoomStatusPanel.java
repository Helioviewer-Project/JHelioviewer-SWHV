package org.helioviewer.jhv.gui.components.statusplugin;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.ProjectionMode;
import org.helioviewer.jhv.gui.Interfaces;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.gui.components.StatusPanel;

@SuppressWarnings("serial")
public final class ZoomStatusPanel extends StatusPanel.StatusPlugin implements Interfaces.LazyComponent {

    private boolean dirty;
    private double cameraWidth;
    private double distance;
    private ProjectionMode mode;

    public ZoomStatusPanel() {
        update(Display.getCamera().getCameraWidth(), Display.getCamera().getViewpoint().distance, Display.mode);
        UITimer.register(this);
    }

    private static String formatOrthoFOV(double r) {
        if (r < 2 * 32 * Sun.Radius)
            return String.format("%6.2fR\u2609", r);
        else
            return String.format("%6.2fau", r * Sun.MeanEarthDistanceInv);
    }

    public void update(double _cameraWidth, double _distance, ProjectionMode _mode) {
        if (cameraWidth == _cameraWidth && distance == _distance && mode == _mode)
            return;

        cameraWidth = _cameraWidth;
        distance = _distance;
        mode = _mode;
        dirty = true;
    }

    @Override
    public void lazyRepaint() {
        if (dirty) {
            String text = mode.isOrthographic()
                    ? String.format("FOV: %s | D\u2609: %7.3fau", formatOrthoFOV(cameraWidth), distance * Sun.MeanEarthDistanceInv)
                    : String.format("| D\u2609: %7.3fau", distance * Sun.MeanEarthDistanceInv);
            setText(text);
            dirty = false;
        }
    }

}
