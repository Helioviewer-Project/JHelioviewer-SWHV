package org.helioviewer.jhv.gui.components.statusplugins;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.BorderFactory;

import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.jhv.gui.components.BasicImagePanel;
import org.helioviewer.jhv.gui.interfaces.ImagePanelPlugin;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.viewport.Viewport;
import org.helioviewer.viewmodel.viewportimagesize.ViewportImageSize;

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

    private View view;
    private JHVJP2View jp2View;

    private BasicImagePanel imagePanel;
    private Point lastPosition;

    private final char PRIME = '\u2032';

    private PositionStatusPanel() {
        setBorder(BorderFactory.createEtchedBorder());
        setPreferredSize(new Dimension(170, 20));

        setText("(x, y) = " + "(    0" + PRIME + PRIME + ",    0" + PRIME + PRIME + ")");

        // Displayer.getLayersModel().addLayersListener(this);
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
        if (jp2View == null || position == lastPosition)
            return;

        // check region and viewport
        Region r = jp2View.getRegion();
        Viewport v = jp2View.getViewport();
        MetaData m = jp2View.getMetaData();

        if (r == null || v == null || m == null) {
            setText("(x, y) = " + "(" + position.x + "," + position.y + ")");
            return;
        }

        // get viewport image size
        ViewportImageSize vis = ViewHelper.calculateViewportImageSize(v, r);

        // Helioviewer images have there physical lower left corner in a
        // negative area; real pixel based image at 0
        if (m.getPhysicalLowerLeft().getX() < 0) {
            Vector2dInt solarcenter = ViewHelper.convertImageToScreenDisplacement(r.getUpperLeftCorner().negateX(), r, vis);
            Vector2dDouble scaling = new Vector2dDouble(Constants.SunRadius, Constants.SunRadius);
            Vector2dDouble solarRadius = new Vector2dDouble(ViewHelper.convertImageToScreenDisplacement(scaling, r, vis));
            Vector2dDouble pos = new Vector2dDouble(position.x - solarcenter.getX(), -position.y + solarcenter.getY()).invertedScale(solarRadius).scale(959.705);

            String text = String.format("(x, y) = (% 5d\u2032\u2032, % 5d\u2032\u2032)", (int) Math.round(pos.getX()), (int) Math.round(pos.getY()));
            setText(text);
        } else {
            // computes pixel position for simple images (e.g. jpg and png)
            // where cursor points at

            // compute coordinates in image
            int x = (int) (r.getWidth() * (position.getX() / vis.getWidth()) + r.getCornerX());
            int y = (int) (m.getPhysicalImageHeight() - (r.getCornerY() + r.getHeight()) + position.getY() / vis.getHeight() * r.getHeight() + 0.5);

            // show coordinates
            setText("(x, y) = " + "(" + x + "," + y + ")");
        }

        lastPosition = position;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View getView() {
        return view;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setView(View newView) {
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void activeLayerChanged(JHVJP2View view) {
        if (view instanceof JHVJP2View) {
            jp2View = view;
            setVisible(true);
        } else {
            setVisible(false);
        }
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
