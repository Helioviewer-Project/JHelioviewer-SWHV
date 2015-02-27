package org.helioviewer.viewmodel.view.bufferedimage;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.AbstractList;

import javax.imageio.ImageIO;

import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.jhv.gui.dialogs.ExportMovieDialog;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.RegionChangedReason;
import org.helioviewer.viewmodel.changeevent.SubImageDataChangedReason;
import org.helioviewer.viewmodel.changeevent.ViewChainChangedReason;
import org.helioviewer.viewmodel.imagedata.JavaBufferedImageData;
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

    private JavaImagePanel javaImagePanel;

    private JavaBufferedImageData imageData;

    // //////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////

    public void updateMainImagePanelSize(Vector2dInt size) {
        javaImagePanel.updateMainImagePanelSize(size);
    }

    public void setOffset(Vector2dInt offset) {
        javaImagePanel.setOffset(offset);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setComponent(Component component) {
        javaImagePanel = (JavaImagePanel) component;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean saveScreenshot(String imageFormat, File outputFile) throws IOException {
        BufferedImage original = imageData.getBufferedImage();
        BufferedImage output = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_3BYTE_BGR);

        Graphics2D g = output.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, original.getWidth(), original.getHeight());
        g.drawImage(original, 0, 0, null);
        ImageIO.write(output, imageFormat, outputFile);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBackgroundColor(Color background) {
        javaImagePanel.setBackgroundColor(background);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void setViewSpecificImplementation(View newView, ChangeEvent changeEvent) {
        if (ViewHelper.getImageDataAdapter(view, JavaBufferedImageData.class) != null) {
            updatePanel();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
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
    @Override
    public AbstractList<ScreenRenderer> getAllPostRenderer() {
        return javaImagePanel.getAllPostRenderer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
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

    @Override
    public void deactivate() {
    }

    @Override
    public void activate() {
    }

    @Override
    public void startExport(ExportMovieDialog exportMovieDialog) {
    }

}
