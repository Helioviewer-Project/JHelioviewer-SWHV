package org.helioviewer.viewmodel.view;

import java.util.ArrayList;
import java.util.HashMap;

import javax.media.opengl.GL2;

import org.helioviewer.gl3d.model.image.GL3DImageLayer;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.LayerChangedReason;
import org.helioviewer.viewmodel.changeevent.LayerChangedReason.LayerChangeType;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.opengl.GL3DView;
import org.helioviewer.viewmodel.view.opengl.shader.GLSLShader;

public class GL3DLayeredView extends AbstractView implements LayeredView, ViewListener, GL3DView {

    protected ArrayList<View> layers = new ArrayList<View>();
    protected HashMap<View, Layer> jp2viewLookup = new HashMap<View, Layer>();
    private final ArrayList<GL3DImageLayer> imageLayers = new ArrayList<GL3DImageLayer>();

    protected class Layer {
        public boolean visibility = true;

        public Layer(View base) {
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isVisible(View view) {
        Layer layer = jp2viewLookup.get(view);

        if (layer != null)
            return layer.visibility;
        else
            return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfVisibleLayer() {
        int result = 0;
        for (Layer layer : jp2viewLookup.values()) {
            if (layer.visibility) {
                result++;
            }
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void toggleVisibility(View view) {
        LinkedMovieManager.getActiveInstance().pauseLinkedMovies();

        Layer layer = jp2viewLookup.get(view);
        if (layer != null) {
            layer.visibility = !layer.visibility;
            notifyViewListeners(new ChangeEvent(new LayerChangedReason(this, LayerChangeType.LAYER_VISIBILITY, view)));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addLayer(View newLayer) {
        addLayer(newLayer, layers.size());
    }

    /**
     * {@inheritDoc}
     */

    @Override
    public void addLayer(View newLayer, int newIndex) {
        if (newLayer == null)
            return;

        LinkedMovieManager.getActiveInstance().pauseLinkedMovies();

        layers.add(newIndex, newLayer);
        jp2viewLookup.put(newLayer, new Layer(newLayer));

        newLayer.addViewListener(this);
        GL3DImageLayer imageLayer = new GL3DImageLayer("", (JHVJP2View) newLayer, true, true, true);
        this.imageLayers.add(imageLayer);
        ChangeEvent event = new ChangeEvent(new LayerChangedReason(this, LayerChangeType.LAYER_ADDED, newLayer));
        notifyViewListeners(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View getLayer(int index) {
        if (index >= 0 && index < layers.size()) {
            return layers.get(index);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumLayers() {
        return layers.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLayerLevel(View view) {
        return layers.indexOf(view);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeLayer(View view) {
        removeLayer(view, true);
    }

    @Override
    public void removeLayer(View view, boolean needAbolish) {
        if (view == null) {
            return;
        }

        int index = layers.indexOf(view);
        if (index == -1) {
            return;
        }

        LinkedMovieManager.getActiveInstance().pauseLinkedMovies();

        layers.remove(view);
        jp2viewLookup.remove(view);
        view.removeViewListener(this);

        JHVJP2View jp2 = view.getAdapter(JHVJP2View.class);
        if (jp2 != null) {
            if (needAbolish) {
                jp2.abolish();
            }
            jp2.removeRenderListener();
        }

        ChangeEvent event = new ChangeEvent(new LayerChangedReason(this, LayerChangeType.LAYER_REMOVED, view, index));
        notifyViewListeners(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeLayer(int index) {
        if (index >= 0 && index < layers.size()) {
            removeLayer(layers.get(index));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAllLayers() {
        LinkedMovieManager.getActiveInstance().pauseLinkedMovies();

        ChangeEvent event = new ChangeEvent();
        for (View view : layers) {
            int index = layers.indexOf(view);
            view.removeViewListener(this);

            JHVJP2View jp2 = view.getAdapter(JHVJP2View.class);
            if (jp2 != null) {
                jp2.abolish();
                jp2.removeRenderListener();
            }

            event.addReason(new LayerChangedReason(this, LayerChangeType.LAYER_REMOVED, view, index));
        }
        layers.clear();
        jp2viewLookup.clear();

        notifyViewListeners(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings(value = { "unchecked" })
    public <T extends View> T getAdapter(Class<T> c) {
        if (c.isInstance(this)) {
            return (T) this;
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void viewChanged(View sender, ChangeEvent event) {
        notifyViewListeners(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void moveView(View view, int newLevel) {
        if (layers.contains(view)) {
            layers.remove(view);
            layers.add(newLevel, view);
            ChangeEvent event = new ChangeEvent(new LayerChangedReason(this, LayerChangeType.LAYER_MOVED, view, newLevel));
            notifyViewListeners(event);
        }
    }

    @Override
    public void render3D(GL3DState state) {
        GL2 gl = state.gl;

        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glBlendEquation(GL2.GL_FUNC_ADD);

        gl.glDisable(GL2.GL_BLEND);
        gl.glEnable(GL2.GL_DEPTH_TEST);

        state.pushMV();
        state.getActiveCamera().applyPerspective(state);
        state.getActiveCamera().applyCamera(state);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

        GLSLShader.bind(gl);
        for (GL3DImageLayer layer : imageLayers) {
            layer.draw(state);
        }
        GLSLShader.unbind(gl);

        gl.glDisable(GL2.GL_BLEND);
        state.getActiveCamera().drawCamera(state);
        state.getActiveCamera().resumePerspective(state);

        state.popMV();

        gl.glEnable(GL2.GL_BLEND);
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
    public void renderGL(GL2 gl, boolean nextView) {
        this.render3D(GL3DState.get());
    }
}
