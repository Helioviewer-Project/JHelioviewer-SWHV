package org.helioviewer.gl3d.view;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.media.opengl.GL;

import org.helioviewer.base.logging.Log;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.jhv.internal_plugins.filter.opacity.DynamicOpacityFilter;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.view.LayeredView;
import org.helioviewer.viewmodel.view.MovieView;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewportView;
import org.helioviewer.viewmodel.view.opengl.GLFilterView;
import org.helioviewer.viewmodel.view.opengl.GLHelioviewerGeometryView;
import org.helioviewer.viewmodel.view.opengl.GLLayeredView;
import org.helioviewer.viewmodel.view.opengl.GLScalePowerOfTwoView;
import org.helioviewer.viewmodel.view.opengl.GLTextureHelper;
import org.helioviewer.viewmodel.view.opengl.GLView;

/**
 * The GL3DLayeredView makes sure to add all required sub-views to a new layer.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DLayeredView extends GLLayeredView implements GL3DView, LayeredView, RegionView, ViewportView {

    public void addLayer(View newLayer, int newIndex) {
        if (newLayer == null)
            return;

        if (!GLTextureHelper.textureNonPowerOfTwoAvailable() && newLayer.getAdapter(GLScalePowerOfTwoView.class) == null) {
            GLScalePowerOfTwoView scaleView = new GLScalePowerOfTwoView();
            scaleView.setView(newLayer);
            newLayer = scaleView;
        }

        if (newLayer.getAdapter(GL3DCoordinateSystemView.class) == null) {
            GL3DCoordinateSystemView coordinateSystemView = new GL3DCoordinateSystemView();
            coordinateSystemView.setView(newLayer);
            newLayer = coordinateSystemView;
        }

        if (newLayer.getAdapter(GL3DImageRegionView.class) == null) {
            GL3DImageRegionView imageRegionView = new GL3DImageRegionView();
            imageRegionView.setView(newLayer);
            newLayer = imageRegionView;
        }

        if (newLayer.getAdapter(GL3DImageTextureView.class) == null) {
            GL3DImageTextureView imageToTextureView = new GL3DImageTextureView();
            imageToTextureView.setView(newLayer);
            newLayer = imageToTextureView;
        }

        insertDynamicOpacityFilter(newLayer);

        // Call to GLLayeredView.addLayer
        super.addLayer(newLayer, newIndex);
    }

    private void insertDynamicOpacityFilter(View newLayer) {
        // Create filter for dynamic view angle dependant opacity
        GLFilterView filterView = new GLFilterView();
        filterView.setFilter(new DynamicOpacityFilter(1.0f));

        // Insert the filter into the chain after GLHelioviewerGeometryView
        GLHelioviewerGeometryView geomView = newLayer.getAdapter(GLHelioviewerGeometryView.class);
        if (geomView != null) {
            filterView.setView(geomView.getView());
            geomView.setView(filterView);
        }
    }

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

    protected boolean recalculateRegionsAndViewports(ChangeEvent event) {
        
        Log.debug("GL3DLayeredView: recalculateRegionsAndViewports: " + this.region + " " + this.viewport);
        boolean changed = false;

        if (viewport != null) {
            for (Layer layer : viewLookup.values()) {
                changed |= layer.viewportView.setViewport(getViewport(), event);
            }
        }
        if (changed) {
            notifyViewListeners(event);
        }
        return changed;
    }

    protected boolean recalculateRegionsAndViewports(ChangeEvent event, boolean includePixelBasedImages) {
        return this.recalculateRegionsAndViewports(event);
    }

    
    protected void redrawBufferImpl() {
    }

    public void renderGL(GL gl) {
        for (int i = 0; i < this.getNumLayers(); i++) {
            View layerView = this.getLayer(i);
            if (layerView instanceof GL3DView) {
            	((GL3DView) layerView).renderGL(gl, true);
            }
        }
    }

}
