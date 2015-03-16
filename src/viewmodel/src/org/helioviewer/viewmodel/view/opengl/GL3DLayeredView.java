package org.helioviewer.viewmodel.view.opengl;

import javax.media.opengl.GL2;

import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.viewmodel.view.AbstractLayeredView;
import org.helioviewer.viewmodel.view.MovieView;
import org.helioviewer.viewmodel.view.View;

/**
 * The GL3DLayeredView makes sure to add all required sub-views to a new layer.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DLayeredView extends AbstractLayeredView implements GL3DView {

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

    public void renderGL(GL2 gl) {
        for (int i = 0; i < this.getNumLayers(); i++) {
            View layerView = this.getLayer(i);
            if (layerView instanceof GL3DView) {
                ((GL3DView) layerView).renderGL(gl, true);
            }
        }
    }

    @Override
    public void renderGL(GL2 gl, boolean nextView) {
        renderGL(gl);
    }

}
