package org.helioviewer.jhv.gui.components.statusplugins;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.JLabel;

import org.helioviewer.base.math.GL3DVec3d;
import org.helioviewer.base.math.MathUtils;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.interfaces.InputControllerPlugin;

/**
 * Status panel for displaying the current mouse position.
 */
@SuppressWarnings({ "serial" })
public class PositionStatusPanel extends JLabel implements MouseMotionListener, InputControllerPlugin {

    private Point lastPosition;

    public PositionStatusPanel() {
        setText("(\u03B8, \u03C6) =( --\u00B0, --\u00B0)");
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

        GL3DVec3d computedposition = Displayer.getViewport().getCamera().getVectorFromSphereAlt(position);
        double radius = Displayer.getViewport().getCamera().getRadiusFromSphereAlt(position);

        if (computedposition == null) {
            setText("(\u03B8, \u03C6) : (--\u00B0, --\u00B0)" + String.format(" | \u03c1 : %.2f R\u2299", radius));
        } else {
            double theta = 90. - Math.acos(computedposition.y) * 180. / Math.PI;
            double phi = 90. - Math.atan2(computedposition.z, computedposition.x) * 180. / Math.PI;
            phi = MathUtils.mapToMinus180To180(phi);
            setText(String.format("(\u03B8, \u03C6) : (%.2f\u00B0,%.2f\u00B0)", theta, phi) +  String.format(" | \u03c1 : %.2f R\u2299", radius));
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
