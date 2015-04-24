package org.helioviewer.jhv.gui.components.statusplugins;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import org.helioviewer.base.math.GL3DVec3d;
import org.helioviewer.gl3d.GL3DState;
import org.helioviewer.jhv.gui.components.MainImagePanel;
import org.helioviewer.jhv.gui.interfaces.ImagePanelPlugin;
import org.helioviewer.viewmodel.view.ComponentView;

/**
 * Status panel for displaying the current mouse position.
 */
public class PositionStatusPanel extends JLabel implements MouseMotionListener, ImagePanelPlugin {

    private static final PositionStatusPanel instance = new PositionStatusPanel();

    private static ComponentView view;
    private static MainImagePanel imagePanel;

    private Point lastPosition;

    private PositionStatusPanel() {
        setBorder(BorderFactory.createEtchedBorder());
        setPreferredSize(new Dimension(170, 20));
        setText("(\u03B8, \u03C6) =( --\u00B0, --\u00B0)");
    }

    public static PositionStatusPanel getSingletonInstance() {
        return instance;
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

        GL3DVec3d computedposition = GL3DState.getActiveCamera().getVectorFromSphereAlt(position);
        double theta = 90. - Math.acos(computedposition.y) * 180. / Math.PI;
        double phi = 90. - Math.atan2(computedposition.z, computedposition.x) * 180. / Math.PI;
        if (computedposition.x * computedposition.x + computedposition.y * computedposition.y > 1.) {
            //setText("(x, y) = " + "(" + String.format("%.2fR\u2609", computedposition.x) + "," + String.format("%.2fR\u2609", computedposition.y) + ")");
            setText("(\u03B8, \u03C6) =( --\u00B0, --\u00B0)");
        } else {
            setText("(\u03B8, \u03C6) = " + "(" + String.format("%.2f\u00B0", theta) + "," + String.format("%.2f\u00B0", phi) + ")");
        }
        lastPosition = position;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ComponentView getView() {
        return view;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setView(ComponentView newView) {
        view = newView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MainImagePanel getImagePanel() {
        return imagePanel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setImagePanel(MainImagePanel newImagePanel) {
        imagePanel = newImagePanel;
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

    public void updatePosition() {
        if (lastPosition != null) {
            updatePosition(lastPosition);
        }
    }

    @Override
    public void detach() {
    }

}
