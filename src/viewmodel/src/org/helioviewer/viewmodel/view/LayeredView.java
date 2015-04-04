package org.helioviewer.viewmodel.view;

import java.util.ArrayList;
import java.util.HashMap;

import org.helioviewer.gl3d.model.image.GL3DImageLayer;
import org.helioviewer.jhv.layers.LayerDescriptor;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

/**
 * View to merged multiple Views.
 *
 * <p>
 * The LayeredView a central element of the view chain. It is responsible for
 * displaying multiple views, which are organized as a stack of layers. The
 * basic functionality this includes to add, move and remove layers.
 *
 * <p>
 * When drawing the different layers, the layer with index zero is drawn first,
 * so the stack of layers is drawn in order bottom to top.
 *
 * <p>
 * The position of the layers in relation to each other is calculated based on
 * their regions. Thus, every view that is connected as a layer must provide a
 * {@link RegionView}.
 *
 * <p>
 * As an additional feature, the LayeredView support hiding layers.
 *
 *
 */

public class LayeredView extends AbstractView implements ViewListener {

    private final LinkedMovieManager movieManager = LinkedMovieManager.getSingletonInstance();

    private final ArrayList<JHVJP2View> layers = new ArrayList<JHVJP2View>();
    private final HashMap<JHVJP2View, Layer> jp2viewLookup = new HashMap<JHVJP2View, Layer>();

    private class Layer {
        public LayerDescriptor ld = new LayerDescriptor();

        public Layer(JHVJP2View view, String name) {
            ImmutableDateTime dt = view.getMetaData().getDateTime();

            ld.isMovie = view instanceof JHVJPXView;
            ld.isMaster = ld.isMovie ? movieManager.isMaster((JHVJPXView) view) : false;
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
            ld.isMaster = ld.isMovie ? movieManager.isMaster((JHVJPXView) view) : false;
            ld.timestamp = ld.isTimed ? view.getMetaData().getDateTime().getCachedDate() : "N/A";
            return ld;
        }
        return null;
    }

    /**
     * Returns whether the given view is visible.
     *
     * If the given view is not a direct child of the LayeredView, returns false
     * in any case.
     *
     * @param view
     *            View to test for visibility
     * @return True if the view is visible
     * @see #toggleVisibility
     */
    public boolean isVisible(JHVJP2View view) {
        Layer layer = jp2viewLookup.get(view);
        if (layer != null)
            return layer.ld.isVisible;
        else
            return false;
    }

    /**
     * Returns number of layers currently visible.
     *
     * This number is lesser or equal to the number of total layers currently
     * connected to the LayeredView.
     *
     * @return Number of visible layers
     * @see #getNumLayers
     */
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
     * Toggles the visibility if the given view.
     *
     * If the given view is not a direct child of the LayeredView, nothing
     * happens.
     *
     * @param view
     *            View to toggle visibility
     * @see #isVisible
     */
    public void toggleVisibility(JHVJP2View view) {
        Layer layer = jp2viewLookup.get(view);
        if (layer != null) {
            movieManager.pauseLinkedMovies();
            layer.ld.isVisible = !layer.ld.isVisible;
        }
    }

    /**
     * Adds a view as a new layer to the LayeredView.
     *
     * The new layer is inserted on top of the current stack, thus will be drawn
     * as last.
     *
     * @param newLayer
     *            View to add as a new layer
     * @see #removeLayer
     */
    public int addLayer(JHVJP2View newLayer) {
        return addLayer(newLayer, layers.size());
    }

    /**
     * Adds a view as a new layer to the LayeredView.
     *
     * The new layer is inserted at the given position of the current stack.
     *
     * @param newLayer
     *            View to add as a new layer
     * @see #removeLayer
     */
    public int addLayer(JHVJP2View newView, int newIndex) {
        if (newView == null)
            return -1;

        movieManager.pauseLinkedMovies();

        GL3DImageLayer imageLayer = new GL3DImageLayer("", newView, true, true, true);
        newView.setImageLayer(imageLayer);
        layers.add(newIndex, newView);
        jp2viewLookup.put(newView, new Layer(newView, imageLayer.getName()));
        newView.addViewListener(this);

        return newIndex;
    }

    /**
     * Returns the view at a given position within the stack of layers.
     *
     * @param index
     *            Position within the stack of layers
     * @return View at given position
     */
    public JHVJP2View getLayer(int index) {
        try {
            return layers.get(index);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns number of layers currently connected to the LayeredView.
     *
     * @return Number of layers
     * @see #getNumberOfVisibleLayer
     */
    public int getNumLayers() {
        return layers.size();
    }

    /**
     * Returns the position of the view within the stack of layers.
     *
     * Zero indicates the most bottom view. If the given view is not a direct
     * child of the LayeredView, the function returns -1.
     *
     * @param view
     *            View to search for within the stack of layers
     * @return Position of the view within stack
     * @see #moveView
     */
    public int getLayerLevel(JHVJP2View view) {
        return layers.indexOf(view);
    }

    /**
     * Removes a layer from the LayeredView.
     *
     * If the given view is not a direct child of the LayeredView, nothing
     * happens.
     *
     * @param view
     *            View to remove from the LayeredView
     * @see #addLayer
     */
    public int removeLayer(JHVJP2View view) {
        return removeLayer(view, true);
    }

    public int removeLayer(JHVJP2View view, boolean needAbolish) {
        if (view == null) {
            return -1;
        }

        int index = layers.indexOf(view);
        if (index == -1) {
            return -1;
        }

        movieManager.pauseLinkedMovies();

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

        return index;
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

}
