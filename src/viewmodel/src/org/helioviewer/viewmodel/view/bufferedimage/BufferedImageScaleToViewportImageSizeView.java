package org.helioviewer.viewmodel.view.bufferedimage;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.SubImageDataChangedReason;
import org.helioviewer.viewmodel.changeevent.ViewportChangedReason;
import org.helioviewer.viewmodel.imagedata.ARGBInt32ImageData;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.imagedata.JavaBufferedImageData;
import org.helioviewer.viewmodel.view.AbstractBasicView;
import org.helioviewer.viewmodel.view.ScaleToViewportImageSizeView;
import org.helioviewer.viewmodel.view.ScalingView;
import org.helioviewer.viewmodel.view.SubimageDataView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.viewportimagesize.ViewportImageSize;

/**
 * Special Software mode view, useful to scale the image to viewport size to
 * avoid distortions in the following operations.
 * 
 * <p>
 * This might be useful for drawing overlays, since they should be independent
 * from the image size but from the screen size.
 * 
 * @author Ludwig Schmidt
 * 
 */
public class BufferedImageScaleToViewportImageSizeView extends AbstractBasicView implements SubimageDataView, ScaleToViewportImageSizeView {

    private ImageData imageData;
    private ScalingView.InterpolationMode interpolationMode;

    /**
     * {@inheritDoc}
     */
    protected void setViewSpecificImplementation(View newView, ChangeEvent changeEvent) {
        scale(changeEvent);
    }

    /**
     * {@inheritDoc}
     */
    public ImageData getSubimageData() {
        return imageData;
    }

    /**
     * {@inheritDoc}
     */
    public void viewChanged(View sender, ChangeEvent aEvent) {
        if (aEvent.reasonOccurred(SubImageDataChangedReason.class) || aEvent.reasonOccurred(ViewportChangedReason.class)) {

            scale(aEvent);
        }
        notifyViewListeners(aEvent);
    }

    /**
     * Rescales the image, if necessary.
     * 
     * Therefore, draws the current image into a new, viewport sized image.
     * 
     * @param aEvent
     *            ChangeEvent to append all changes following
     */
    private void scale(ChangeEvent aEvent) {
        ViewportImageSize viewportImageSize = ViewHelper.calculateViewportImageSize(view);
        JavaBufferedImageData original = ViewHelper.getImageDataAdapter(view, JavaBufferedImageData.class);

        if (viewportImageSize == null || original == null) {
            return;
        }

        if (viewportImageSize.getWidth() == original.getWidth() && viewportImageSize.getHeight() == original.getHeight()) {
            imageData = original;
            return;
        }

        if (viewportImageSize.getWidth() == 0 || viewportImageSize.getHeight() == 0) {
            return;
        }

        BufferedImage img = new BufferedImage(viewportImageSize.getWidth(), viewportImageSize.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        Object renderingHint = ViewHelper.ConvertScaleInterpolationModeToRenderingHint(interpolationMode);
        if (renderingHint != null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, renderingHint);
        }

        g.drawImage(original.getBufferedImage(), 0, 0, viewportImageSize.getWidth(), viewportImageSize.getHeight(), null);
        imageData = new ARGBInt32ImageData(img, original.getColorMask());

        aEvent.addReason(new SubImageDataChangedReason(this));
    }

    /**
     * {@inheritDoc}
     */
    public InterpolationMode getInterpolationMode() {
        return interpolationMode;
    }

    /**
     * {@inheritDoc}
     */
    public void setInterpolationMode(InterpolationMode newInterpolationMode) {
        interpolationMode = newInterpolationMode;
        if (view != null) {
            ChangeEvent changeEvent = new ChangeEvent();
            scale(changeEvent);
            notifyViewListeners(changeEvent);
        }
    }
}
