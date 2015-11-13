package org.helioviewer.jhv.gui.components.statusplugins;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.camera.Viewport;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.StatusPanel;
import org.helioviewer.jhv.gui.interfaces.InputControllerPlugin;

@SuppressWarnings("serial")
public class PositionStatusPanel extends StatusPanel.StatusPlugin implements MouseMotionListener, InputControllerPlugin {

    private Point lastPosition;
    private static String rhoFormat = " | \u03c1 : %.2f R\u2299";
    private static String emptyPos = "(\u03B8, \u03C6) : (--\u00B0, --\u00B0)";

    public PositionStatusPanel() {
        setText(emptyPos + String.format(rhoFormat, 0.));
    }

    /**
     * Updates the displayed position.
     *
     * If the physical dimensions are available, translates the screen
     * coordinates to physical coordinates.
     *
     * @param position
     *            Position on the screen.
     */
    private void updatePosition(Point position) {
        if (position == lastPosition)
            return;

        Camera camera = Displayer.getCamera();
        Viewport vp = Displayer.getViewport();

        Vec3 computedposition = CameraHelper.getVectorFromSphereAlt(camera, vp, position);
        double radius = CameraHelper.getRadiusFromSphereAlt(camera, vp, position);

        if (computedposition == null) {
            setText(emptyPos + String.format(rhoFormat, radius));
        } else {
            double theta = 90. - Math.acos(computedposition.y) * 180. / Math.PI;
            double phi = 90. - Math.atan2(computedposition.z, computedposition.x) * 180. / Math.PI;
            phi = MathUtils.mapToMinus180To180(phi);
            setText(String.format("(\u03B8, \u03C6) : (%.2f\u00B0,%.2f\u00B0)", theta, phi) + String.format(rhoFormat, radius));
        }
        lastPosition = position;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setComponent(Component _component) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        updatePosition(e.getPoint());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        updatePosition(e.getPoint());
    }

}
