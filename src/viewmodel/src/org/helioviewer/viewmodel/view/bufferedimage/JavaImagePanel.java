package org.helioviewer.viewmodel.view.bufferedimage;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.AbstractList;
import java.util.LinkedList;

import javax.swing.JPanel;

import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.viewmodel.renderer.screen.BufferedImageScreenRenderGraphics;
import org.helioviewer.viewmodel.renderer.screen.ScreenRenderer;

/**
 * Panel which displays the via software created image.
 */
public class JavaImagePanel extends JPanel {
    protected Vector2dInt mainImagePanelSize;

    // //////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;

    private BufferedImage image;
    private Color backgroundColor = Color.BLACK;
    private Vector2dInt offset = new Vector2dInt();
    private final LinkedList<ScreenRenderer> postRenderers = new LinkedList<ScreenRenderer>();

    // //////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////
    /**
     * Set the latest image data to the panel.
     *
     * @param aImage
     *            Image to draw to the screen.
     */
    public void setImage(BufferedImage aImage) {

        image = aImage;
    }

    /**
     * Sets the background color.
     *
     * This color will be displayed in areas with no image, where images are
     * transparent or when there is no image present at all.
     *
     * @param color
     *            new background color
     */
    public void setBackgroundColor(Color color) {
        backgroundColor = color;
    }

    /**
     * Sets the displacement of the upper left corner of the image relative to
     * the component.
     *
     * For example, this function can be used to manually center the image.
     *
     * @param _offset
     *            new offset
     */
    public void setOffset(Vector2dInt _offset) {
        offset = _offset;
    }

    public void updateMainImagePanelSize(Vector2dInt size) {
        mainImagePanelSize = size;
    }

    /**
     * Adds a post renderer, which can draw simple geometric forms on a drawn
     * image and background.
     *
     * The post renderer will be called after every redraw of the actual image.
     *
     * @param postRenderer
     *            new post renderer
     * @see #removePostRenderer(ScreenRenderer)
     * @see #getAllPostRenderer()
     */
    public void addPostRenderer(ScreenRenderer postRenderer) {
        if (postRenderer != null)
            postRenderers.add(postRenderer);
    }

    /**
     * Removes a post renderer.
     *
     * @param postRenderer
     *            post renderer which should be removed
     * @see #addPostRenderer(ScreenRenderer)
     * @see #getAllPostRenderer()
     */
    public void removePostRenderer(ScreenRenderer postRenderer) {
        if (postRenderer != null) {
            do {
                postRenderers.remove(postRenderer);
            } while (postRenderers.contains(postRenderer));
        }
    }

    /**
     * Returns the list of all post renderer.
     *
     * This function can be used to move the set of post renderers from one
     * ComponentView to another.
     *
     * @return list of all post renderer
     * @see #addPostRenderer(ScreenRenderer)
     * @see #removePostRenderer(ScreenRenderer)
     */
    public AbstractList<ScreenRenderer> getAllPostRenderer() {
        return postRenderers;
    }

    /**
     * Draws the current image to the screen. Calls all registered post renderer
     * afterwards.
     */
    @Override
    public void paintComponent(Graphics g) {
        // draw image to panel
        g.setColor(backgroundColor);
        g.fillRect(0, 0, getWidth(), getHeight());

        int xOffsetFinal = offset.getX();
        int yOffsetFinal = offset.getY();
        if (mainImagePanelSize != null && image != null) {
            if (image.getHeight() < mainImagePanelSize.getY()) {
                yOffsetFinal += (mainImagePanelSize.getY() - image.getHeight()) / 2;
            }
            if (image.getWidth() < mainImagePanelSize.getX()) {
                xOffsetFinal += (mainImagePanelSize.getX() - image.getWidth()) / 2;
            }
        }
        g.drawImage(image, xOffsetFinal, yOffsetFinal, null);

        // execute the post renderer
        BufferedImageScreenRenderGraphics gRenderer = new BufferedImageScreenRenderGraphics((Graphics2D) g);
        for (ScreenRenderer r : postRenderers) {
            r.render(gRenderer);
        }
    }
}
