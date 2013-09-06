package org.helioviewer.viewmodel.view.bufferedimage;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.AbstractList;
import java.util.LinkedList;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.RegionChangedReason;
import org.helioviewer.viewmodel.changeevent.SubImageDataChangedReason;
import org.helioviewer.viewmodel.changeevent.ViewChainChangedReason;
import org.helioviewer.viewmodel.imagedata.JavaBufferedImageData;
import org.helioviewer.viewmodel.renderer.screen.BufferedImageScreenRenderGraphics;
import org.helioviewer.viewmodel.renderer.screen.ScreenRenderer;
import org.helioviewer.viewmodel.view.AbstractComponentView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.view.ViewListener;

/**
 * Implementation of ComponentView for rendering in software mode.
 * 
 * <p>
 * This class manages a JPanel, used to draw the screen.
 * 
 * <p>
 * For further information about the role of the ComponentView within the view
 * chain, see {@link org.helioviewer.viewmodel.view.ComponentView}
 * 
 * @author Stephan Pagel
 * 
 *         TODO: When drawing the image to the screen, this class does not take
 *         into account the value of the color mask. This has to be be done.
 */
public class BufferedImageComponentView extends AbstractComponentView {

    // //////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////

    private JavaImagePanel javaImagePanel = new JavaImagePanel();

    private JavaBufferedImageData imageData;

    // //////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public Component getComponent() {
        return javaImagePanel;
    }

    /**
     * {@inheritDoc}
     */
    public void saveScreenshot(String imageFormat, File outputFile) throws IOException {
        BufferedImage original = imageData.getBufferedImage();
        BufferedImage output = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_3BYTE_BGR);

        Graphics2D g = output.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, original.getWidth(), original.getHeight());
        g.drawImage(original, 0, 0, null);

        ImageIO.write(output, imageFormat, outputFile);
    }

    /**
     * {@inheritDoc}
     */
    public void setBackgroundColor(Color background) {
        javaImagePanel.setBackgroundColor(background);
    }

    /**
     * {@inheritDoc}
     */
    public void setOffset(Vector2dInt offset) {
        javaImagePanel.setOffset(offset);
    }

    /**
     * {@inheritDoc}
     */
    protected void setViewSpecificImplementation(View newView, ChangeEvent changeEvent) {
        if (ViewHelper.getImageDataAdapter(view, JavaBufferedImageData.class) != null) {
            updatePanel();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addPostRenderer(ScreenRenderer postRenderer) {
        if (postRenderer != null) {
            javaImagePanel.addPostRenderer(postRenderer);

            if (postRenderer instanceof ViewListener) {
                addViewListener((ViewListener) postRenderer);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removePostRenderer(ScreenRenderer postRenderer) {
        if (postRenderer != null) {
            javaImagePanel.removePostRenderer(postRenderer);

            if (postRenderer instanceof ViewListener) {
                removeViewListener((ViewListener) postRenderer);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public AbstractList<ScreenRenderer> getAllPostRenderer() {
        return javaImagePanel.getAllPostRenderer();
    }

    /**
     * {@inheritDoc}
     */
    public void viewChanged(View sender, ChangeEvent aEvent) {

        if (aEvent.reasonOccurred(RegionChangedReason.class) || aEvent.reasonOccurred(SubImageDataChangedReason.class) || aEvent.reasonOccurred(ViewChainChangedReason.class)) {

            updatePanel();
        }
        // inform all listener of the latest change reason
        notifyViewListeners(aEvent);
    }

    /**
     * Updates the JPanel to draw the current image.
     */
    private void updatePanel() {
        // get the latest image data
        imageData = ViewHelper.getImageDataAdapter(view, JavaBufferedImageData.class);

        // pass latest image data to the panel where data shall be displayed
        if (imageData != null) {
            javaImagePanel.setImage(imageData.getBufferedImage());
            javaImagePanel.repaint();
        }
    }

    public void deactivate() {
    };

    public void activate() {
    }

    /**
     * Panel which displays the via software created image.
     */
    private class JavaImagePanel extends JPanel {

        // //////////////////////////////////////////////////////////////
        // Definitions
        // //////////////////////////////////////////////////////////////

        private static final long serialVersionUID = 1L;

        private BufferedImage image;
        private Color backgroundColor = Color.BLACK;
        private Vector2dInt offset = new Vector2dInt();
        private LinkedList<ScreenRenderer> postRenderers = new LinkedList<ScreenRenderer>();

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
         * Sets the displacement of the upper left corner of the image relative
         * to the component.
         * 
         * For example, this function can be used to manually center the image.
         * 
         * @param _offset
         *            new offset
         */
        public void setOffset(Vector2dInt _offset) {
            offset = _offset;
        }

        /**
         * Adds a post renderer, which can draw simple geometric forms on a
         * drawn image and background.
         * 
         * The post renderer will be called after every redraw of the actual
         * image.
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
         * Draws the current image to the screen. Calls all registered post
         * renderer afterwards.
         */
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

}
