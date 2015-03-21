package org.helioviewer.viewmodel.view;

import java.util.ArrayList;
import java.util.HashMap;

import javax.media.opengl.GL2;

import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.LayerChangedReason;
import org.helioviewer.viewmodel.changeevent.LayerChangedReason.LayerChangeType;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.opengl.GL3DImageTextureView;
import org.helioviewer.viewmodel.view.opengl.GL3DView;

public class GL3DLayeredView extends AbstractView implements LayeredView, ViewListener, GL3DView {

    protected ArrayList<View> layers = new ArrayList<View>();
    protected HashMap<View, Layer> viewLookup = new HashMap<View, Layer>();

    protected class Layer {
        public final View view;

        public JHVJP2View jp2View;

        public boolean visibility = true;

        public Layer(View base) {
            view = base;
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isVisible(View view) {
        Layer layer = viewLookup.get(view);

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
        for (Layer layer : viewLookup.values()) {
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

        Layer layer = viewLookup.get(view);
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

        if (newLayer.getAdapter(GL3DImageTextureView.class) == null) {
            GL3DImageTextureView imageToTextureView = new GL3DImageTextureView();
            imageToTextureView.setView(newLayer);
            newLayer = imageToTextureView;
        }

        LinkedMovieManager.getActiveInstance().pauseLinkedMovies();

        layers.add(newIndex, newLayer);
        newLayer.addViewListener(this);
        viewLookup.put(newLayer, new Layer(newLayer));

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
        viewLookup.remove(view);
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
        viewLookup.clear();

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
    }
}
