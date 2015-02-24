package org.helioviewer.gl3d.view;

import javax.media.opengl.GL2;

import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.MovieView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.view.opengl.GLLayeredView;
import org.helioviewer.viewmodel.view.opengl.GLView;
import org.helioviewer.viewmodel.viewport.StaticViewport;
import org.helioviewer.viewmodel.viewport.Viewport;
import org.helioviewer.viewmodel.viewport.ViewportAdapter;

/**
 * The GL3DLayeredView makes sure to add all required sub-views to a new layer.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DLayeredView extends GLLayeredView implements GL3DView {

    @Override
    public void addLayer(View newLayer, int newIndex) {
        if (newLayer == null)
            return;

        if (newLayer.getAdapter(GL3DImageTextureView.class) == null) {
            GL3DImageTextureView imageToTextureView = new GL3DImageTextureView();
            imageToTextureView.setView(newLayer);
            newLayer = imageToTextureView;
        }

        // Call to GLLayeredView.addLayer
        super.addLayer(newLayer, newIndex);
    }

    @Override
    public void render3D(GL3DState state) {
        for (int i = 0; i < this.getNumLayers(); i++) {
            View layerView = this.getLayer(i);
            if (layerView instanceof GL3DView) {
                ((GL3DView) layerView).render3D(state);
            } else if (layerView instanceof GLView) {
                ((GLView) layerView).renderGL(state.gl, true);
            }
        }
    }

    @Override
    public void deactivate(GL3DState state) {
        for (int i = 0; i < getNumLayers(); i++) {
            if (getLayer(i).getAdapter(GL3DView.class) != null) {
                MovieView movieView = getLayer(i).getAdapter(MovieView.class);
                if (movieView != null) {
                    movieView.pauseMovie();
                }
                getLayer(i).getAdapter(GL3DView.class).deactivate(state);
            }
        }
    }

    @Override
    protected void redrawBufferImpl() {
    }

    public void renderGL(GL2 gl) {
        for (int i = 0; i < this.getNumLayers(); i++) {
            View layerView = this.getLayer(i);
            if (layerView instanceof GL3DView) {
                ((GL3DView) layerView).renderGL(gl, true);
            }
        }
    }

    /**
     * Recalculates the regions and viewports of all layers.
     *
     * <p>
     * Sets the regions and viewports of all layers according to the region and
     * viewport of the LayeredView. Also, calculates the offset of the layers to
     * each other.
     *
     * @param event
     *            ChangeEvent to collect history of all following changes
     * @return true, if at least one region or viewport changed
     */
    @Override
    protected boolean recalculateRegionsAndViewports(ChangeEvent event, boolean includePixelBasedImages) {
        boolean changed = false;
        if (region == null && metaData != null) {
            region = StaticRegion.createAdaptedRegion(metaData.getPhysicalRectangle());
        }

        if (viewport != null && region != null) {
            viewportImageSize = ViewHelper.calculateViewportImageSize(viewport, region);
            layerLock.lock();
            {
                for (Layer layer : viewLookup.values()) {
                    Viewport layerViewport = new ViewportAdapter(new StaticViewport(viewportImageSize.getWidth(), viewportImageSize.getHeight()));
                    changed |= layer.viewportView.setViewport(layerViewport, event);
                }
            }
            layerLock.unlock();
        }

        return changed;
    }
}
