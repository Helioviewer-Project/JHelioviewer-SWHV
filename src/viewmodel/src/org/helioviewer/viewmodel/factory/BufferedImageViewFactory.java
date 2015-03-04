package org.helioviewer.viewmodel.factory;

import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.LayeredView;
import org.helioviewer.viewmodel.view.OverlayView;
import org.helioviewer.viewmodel.view.ScaleToViewportImageSizeView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.bufferedimage.BufferedImageComponentView;
import org.helioviewer.viewmodel.view.bufferedimage.BufferedImageLayeredView;
import org.helioviewer.viewmodel.view.bufferedimage.BufferedImageOverlayView;
import org.helioviewer.viewmodel.view.bufferedimage.BufferedImageScaleToViewportImageSizeView;

/**
 * Implementation of interface ViewFactory for Java BufferedImage views.
 * 
 * <p>
 * This class implements the interface ViewFactory in such a way, that it
 * returns a Java BufferedImage specific implementation or a independent
 * standard implementation, if no specific one is available.
 * <p>
 * For further details on how to use view factories, see {@link ViewFactory}.
 * 
 * @author Markus Langenberg
 * 
 */
public class BufferedImageViewFactory extends StandardViewFactory {

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T extends View> T createNewView(Class<T> pattern) {

        // ComponentView
        if (pattern.isAssignableFrom(ComponentView.class)) {
            return (T) new BufferedImageComponentView();
            // OverlayView
        } else if (pattern.isAssignableFrom(OverlayView.class)) {
            return (T) new BufferedImageOverlayView();
            // LayeredView
        } else if (pattern.isAssignableFrom(LayeredView.class)) {
            return (T) new BufferedImageLayeredView();
            // ScaleToViewportImageSizeView
        } else if (pattern.isAssignableFrom(ScaleToViewportImageSizeView.class)) {
            return (T) new BufferedImageScaleToViewportImageSizeView();
        } else {
            return super.createNewView(pattern);
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T extends View> T createViewFromSourceImpl(T source) {

        // ComponentView
        if (source instanceof ComponentView) {
            return (T) new BufferedImageComponentView();
            // OverlayView
        } else if (source instanceof OverlayView) {
            return (T) new BufferedImageOverlayView();
            // LayeredView
        } else if (source instanceof LayeredView) {
            return (T) new BufferedImageLayeredView();
            // ScaleToViewportImageSizeView
        } else if (source instanceof ScaleToViewportImageSizeView) {
            return (T) new BufferedImageScaleToViewportImageSizeView();
        } else {
            return createStandardViewFromSource(source);
        }
    }

}
