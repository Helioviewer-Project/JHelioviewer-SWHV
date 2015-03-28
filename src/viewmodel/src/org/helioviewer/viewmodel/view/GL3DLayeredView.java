package org.helioviewer.viewmodel.view;

import java.util.ArrayList;
import java.util.HashMap;

import javax.media.opengl.GL2;

import org.helioviewer.gl3d.model.image.GL3DImageLayer;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.layers.LayerDescriptor;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.LayerChangedReason;
import org.helioviewer.viewmodel.changeevent.LayerChangedReason.LayerChangeType;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;
import org.helioviewer.viewmodel.view.opengl.GL3DView;

public class GL3DLayeredView extends AbstractView implements LayeredView, ViewListener, GL3DView {

    protected ArrayList<JHVJP2View> layers = new ArrayList<JHVJP2View>();
    protected HashMap<JHVJP2View, Layer> jp2viewLookup = new HashMap<JHVJP2View, Layer>();

    protected class Layer {
        public LayerDescriptor ld = new LayerDescriptor();

        public Layer(JHVJP2View view, String name) {
            ImmutableDateTime dt = view.getMetaData().getDateTime();

            ld.isMovie = view instanceof JHVJPXView;
            ld.isMaster = ld.isMovie ? LinkedMovieManager.getActiveInstance().isMaster((JHVJPXView) view) : false;
            ld.isVisible = true;
            ld.isTimed = dt != null;
            ld.title = name;
            ld.timestamp = ld.isTimed ? dt.getCachedDate() : "N/A";
        }
    }

    public LayerDescriptor getLayerDescriptor(JHVJP2View view) {
        Layer layer = jp2viewLookup.get(view);
        if (layer != null) {
            LayerDescriptor ld = layer.ld;
            ld.isMaster = ld.isMovie ? LinkedMovieManager.getActiveInstance().isMaster((JHVJPXView) view) : false;
            ld.timestamp = ld.isTimed ? view.getMetaData().getDateTime().getCachedDate() : "N/A";
            return ld;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isVisible(JHVJP2View view) {
        Layer layer = jp2viewLookup.get(view);
        if (layer != null)
            return layer.ld.isVisible;
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
            if (layer.ld.isVisible) {
                result++;
            }
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void toggleVisibility(JHVJP2View view) {
        LinkedMovieManager.getActiveInstance().pauseLinkedMovies();

        Layer layer = jp2viewLookup.get(view);
        if (layer != null) {
            layer.ld.isVisible = !layer.ld.isVisible;
            notifyViewListeners(new ChangeEvent(new LayerChangedReason(this, LayerChangeType.LAYER_VISIBILITY, view)));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addLayer(JHVJP2View newLayer) {
        addLayer(newLayer, layers.size());
    }

    /**
     * {@inheritDoc}
     */

    @Override
    public void addLayer(JHVJP2View newView, int newIndex) {
        if (newView == null)
            return;

        LinkedMovieManager.getActiveInstance().pauseLinkedMovies();

        GL3DImageLayer imageLayer = new GL3DImageLayer("", newView, true, true, true);

        layers.add(newIndex, newView);
        jp2viewLookup.put(newView, new Layer(newView, imageLayer.getName()));
        newView.addViewListener(this);

        ChangeEvent event = new ChangeEvent(new LayerChangedReason(this, LayerChangeType.LAYER_ADDED, newView));
        notifyViewListeners(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JHVJP2View getLayer(int index) {
        try {
            return layers.get(index);
        } catch (Exception e) {
            return null;
        }
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
    public int getLayerLevel(JHVJP2View view) {
        return layers.indexOf(view);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeLayer(JHVJP2View view) {
        removeLayer(view, true);
    }

    @Override
    public void removeLayer(JHVJP2View view, boolean needAbolish) {
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
    public void moveView(JHVJP2View view, int newLevel) {
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
        Displayer.getRenderablecontainer().render(state);
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
