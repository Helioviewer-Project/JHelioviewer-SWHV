package org.helioviewer.viewmodel.view;

import java.util.ArrayList;

import org.helioviewer.gl3d.model.image.GL3DImageLayer;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;

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
