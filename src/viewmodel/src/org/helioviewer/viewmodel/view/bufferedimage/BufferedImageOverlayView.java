package org.helioviewer.viewmodel.view.bufferedimage;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.concurrent.ConcurrentSkipListSet;

import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.RegionChangedReason;
import org.helioviewer.viewmodel.changeevent.SubImageDataChangedReason;
import org.helioviewer.viewmodel.changeevent.ViewChainChangedReason;
import org.helioviewer.viewmodel.imagedata.ARGBInt32ImageData;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.imagedata.JavaBufferedImageData;
import org.helioviewer.viewmodel.renderer.physical.BufferedImagePhysicalRenderGraphics;
import org.helioviewer.viewmodel.renderer.physical.PhysicalRenderer;
import org.helioviewer.viewmodel.view.AbstractBasicView;
import org.helioviewer.viewmodel.view.LayeredView;
import org.helioviewer.viewmodel.view.OverlayView;
import org.helioviewer.viewmodel.view.ScaleToViewportImageSizeView;
import org.helioviewer.viewmodel.view.SubimageDataView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.view.ScalingView.InterpolationMode;
import org.helioviewer.viewmodel.view.opengl.OverlayPluginContainer;

/**
 * Implementation of OverlayView for rendering in software mode.
 * 
 * <p>
 * This class provides the capability to draw overlays in software mode.
 * Therefore it manages a {@link PhysicalRenderer}, which is passed to the
 * registered renderer.
 * 
 * <p>
 * For further information about the role of the OverlayView within the view
 * chain, see {@link org.helioviewer.viewmodel.view.OverlayView}.
 * 
 * @author Markus Langenberg
 */
public class BufferedImageOverlayView extends AbstractBasicView implements OverlayView, SubimageDataView {

    private SubimageDataView subimageDataView;
    private LayeredView layeredView;
    private ImageData imageData;
    private PhysicalRenderer overlayRenderer;

    /**
     * {@inheritDoc}
     */
    public void setRenderer(PhysicalRenderer renderer) {
        overlayRenderer = renderer;
    }

    /**
     * {@inheritDoc}
     */
    public PhysicalRenderer getRenderer() {
        return overlayRenderer;
    }

    /**
     * {@inheritDoc}
     */
    public void setView(View newView) {
        // If no ScaleToViewportImageSizeView present, insert it
        if (newView != null && newView.getAdapter(ScaleToViewportImageSizeView.class) == null) {
            ScaleToViewportImageSizeView scaleToViewportImageSizeView = new BufferedImageScaleToViewportImageSizeView();
            scaleToViewportImageSizeView.setInterpolationMode(InterpolationMode.BILINEAR);
            scaleToViewportImageSizeView.setView(newView);

            // use scaleToViewportImageSizeView as follower
            newView = scaleToViewportImageSizeView;
        }

        super.setView(newView);
    }

    /**
     * {@inheritDoc}
     */
    protected void setViewSpecificImplementation(View newView, ChangeEvent changeEvent) {
        subimageDataView = ViewHelper.getViewAdapter(view, SubimageDataView.class);
        layeredView = ViewHelper.getViewAdapter(view, LayeredView.class);

        if (ViewHelper.getImageDataAdapter(subimageDataView, JavaBufferedImageData.class) != null) {
            drawOverlays();
        }
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

        if (aEvent.reasonOccurred(ViewChainChangedReason.class)) {
            subimageDataView = ViewHelper.getViewAdapter(view, SubimageDataView.class);
            layeredView = ViewHelper.getViewAdapter(view, LayeredView.class);
        }

        if (aEvent.reasonOccurred(RegionChangedReason.class) || aEvent.reasonOccurred(SubImageDataChangedReason.class)) {
            drawOverlays();
        }

        notifyViewListeners(aEvent);
    }

    /**
     * Draws the overlays to the image.
     * 
     * Therefore, calls the registered renderer.
     */
    private void drawOverlays() {
        // if no renderer registered, just pass image data
        if (overlayRenderer == null || layeredView.getNumLayers() <= 0) {
            imageData = subimageDataView.getSubimageData();
            return;
        }

        // get source image data
        JavaBufferedImageData sourceData = ViewHelper.getImageDataAdapter(subimageDataView, JavaBufferedImageData.class);
        if (sourceData == null) {
            return;
        }

        // get buffered image
        BufferedImage source = sourceData.getBufferedImage();
        if (source == null) {
            return;
        }

        // copy original image
        BufferedImage target = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = target.getGraphics();
        g.drawImage(source, 0, 0, null);

        // render overlays to image
        BufferedImagePhysicalRenderGraphics renderGraphics = new BufferedImagePhysicalRenderGraphics(g, view);
        overlayRenderer.render(renderGraphics);

        imageData = new ARGBInt32ImageData(sourceData, target);
    }



	@Override
	public void addOverlay(OverlayPluginContainer overlayPluginContainer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ConcurrentSkipListSet<OverlayPluginContainer> getOverlays() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeOverlay(int index) {
		// TODO Auto-generated method stub
		
	}
}
