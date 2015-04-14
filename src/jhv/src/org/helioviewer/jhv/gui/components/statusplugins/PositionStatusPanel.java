package org.helioviewer.jhv.gui.components.statusplugins;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.BorderFactory;

import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.jhv.gui.components.BasicImagePanel;
import org.helioviewer.jhv.gui.interfaces.ImagePanelPlugin;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;

/**
 * Status panel for displaying the current mouse position.
 *
 * <p>
 * If the the physical dimension of the image are known, the physical position
 * will be shown, otherwise, shows the screen position.
 *
 * <p>
 * Basically, the information of this panel is independent from the active
 * layer.
 *
 * <p>
 * If there is no layer present, this panel will be invisible.
 */
public class PositionStatusPanel extends ViewStatusPanelPlugin implements MouseMotionListener, ImagePanelPlugin {

    private static final long serialVersionUID = 1L;
    private static final PositionStatusPanel instance = new PositionStatusPanel();

    private ComponentView view;
    private JHVJP2View jp2View;

    private BasicImagePanel imagePanel;
    private Point lastPosition;

    private final char PRIME = '\u2032';

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
        GL3DState state = GL3DState.get();
        if (position == lastPosition || state == null)
            return;
        GL3DCamera camera = state.getActiveCamera();
        if (camera == null)
            return;

        GL3DVec3d computedposition = camera.getVectorFromSphereAlt(position);
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
    public BasicImagePanel getImagePanel() {
        return imagePanel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setImagePanel(BasicImagePanel newImagePanel) {
        if (imagePanel != null) {
            imagePanel.removeMouseMotionListener(this);
        }
        imagePanel = newImagePanel;
        imagePanel.addMouseMotionListener(this);
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
