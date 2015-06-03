package org.helioviewer.jhv.layers;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import org.helioviewer.base.datetime.ImmutableDateTime;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.renderable.components.RenderableImageLayer;
import org.helioviewer.viewmodel.view.AbstractView;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;

public class LayersModel {

    private static AbstractView activeView;
    private static final ArrayList<AbstractView> layers = new ArrayList<AbstractView>();

    /**
     * Returns the view at a given position within the stack of layers.
     *
     * @param index
     *            Position within the stack of layers
     * @return View at given position
     */
    private static AbstractView getLayer(int index) {
        try {
            return layers.get(index);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns number of layers
     *
     * @return Number of layers
     * @see #getNumberOfVisibleLayer
     */
    public static int getNumLayers() {
        return layers.size();
    }

    /**
     * Return the view associated with the active Layer
     *
     * @return View associated with the active Layer
     */
    public static AbstractView getActiveView() {
        return activeView;
    }

    public static void setActiveView(AbstractView view) {
        activeView = view;
        fireActiveLayerChanged(view);
    }

    private static ImmutableDateTime getStartDateImmutable(AbstractView view) {
        ImmutableDateTime result = null;

        if (view instanceof JHVJPXView) {
            result = ((JHVJPXView) view).getFrameDateTime(0);
        } else {
            result = view.getMetaData().getDateObs();
        }
        return result;
    }

    private static ImmutableDateTime getEndDateImmutable(AbstractView view) {
        ImmutableDateTime result = null;

        if (view instanceof JHVJPXView) {
            JHVJPXView tmv = (JHVJPXView) view;
            int lastFrame = tmv.getMaximumFrameNumber();
            result = tmv.getFrameDateTime(lastFrame);
        } else {
            result = view.getMetaData().getDateObs();
        }
        return result;
    }

    private static ImmutableDateTime getStartDateImmutable(int idx) {
        return getStartDateImmutable(getLayer(idx));
    }

    private static ImmutableDateTime getEndDateImmutable(int idx) {
        return getEndDateImmutable(getLayer(idx));
    }

    /**
     * Return the timestamp of the first available image data of the layer in
     * question
     *
     * @param view
     *            - View that can be associated with the layer in question
     * @return timestamp of the first available image data, null if no
     *         information available
     */
    public static Date getStartDate(AbstractView view) {
        Date result = null;
        ImmutableDateTime date = getStartDateImmutable(view);

        if (date != null)
            result = date.getTime();
        return result;
    }

    /**
     * Return the timestamp of the last available image data of the layer in
     * question
     *
     * @param view
     *            - View that can be associated with the layer in question
     * @return timestamp of the last available image data, null if no
     *         information available
     */
    public static Date getEndDate(AbstractView view) {
        Date result = null;
        ImmutableDateTime date = getEndDateImmutable(view);

        if (date != null)
            result = date.getTime();
        return result;
    }

    /**
     * Return the timestamp of the first available image data
     *
     * @return timestamp of the first available image data, null if no
     *         information available
     */
    public static Date getFirstDate() {
        ImmutableDateTime earliest = null;

        int size = layers.size();
        for (int idx = 0; idx < size; idx++) {
            ImmutableDateTime start = getStartDateImmutable(idx);
            if (start == null) {
                continue;
            }
            if (earliest == null || start.compareTo(earliest) < 0) {
                earliest = start;
            }
        }
        return earliest == null ? null : earliest.getTime();
    }

    /**
     * Return the timestamp of the last available image data
     *
     * @return timestamp of the last available image data, null if no
     *         information available
     */
    public static Date getLastDate() {
        ImmutableDateTime latest = null;

        int size = layers.size();
        for (int idx = 0; idx < size; idx++) {
            ImmutableDateTime end = getEndDateImmutable(idx);
            if (end == null) {
                continue;
            }
            if (latest == null || end.compareTo(latest) > 0) {
                latest = end;
            }
        }
        return latest == null ? null : latest.getTime();
    }

    public static void addLayer(AbstractView view) {
        if (view == null)
            return;

        // needed for proper linked movies (tbd)
        LinkedMovieManager.pauseLinkedMovies();

        view.setImageLayer(new RenderableImageLayer(view));
        layers.add(view);

        MoviePanel moviePanel = MoviePanel.getSingletonInstance().setView(view);
        view.setMoviePanel(moviePanel);
        MoviePanel.getMoviePanelManager().linkView(view);

        ImageViewerGui.getMoviePanelContainer().addLayer(view, moviePanel);

        fireLayerAdded(view);
        setActiveView(view);
    }

    // special
    public static void addLayerFromThread(AbstractView view) {
        EventQueue.invokeLater(new Runnable() {
            private AbstractView theView;

            @Override
            public void run() {
                addLayer(theView);
            }

            public Runnable init(AbstractView _view) {
                theView = _view;
                return this;
            }
        }.init(view));
    }

    /**
     * Check if the given index is valid, given the current state of the
     * ViewChain
     *
     * @param idx
     *            - index of the layer in question
     * @return true if the index is valid
     */
    private static boolean isValidIndex(int idx) {
        if (idx >= 0 && idx < layers.size()) {
            return true;
        }
        return false;
    }

    /**
     * Calculate a new activeLayer after the old Layer has been deleted
     *
     * @param oldActiveLayerIdx
     *            - index of old active, but deleted, layer
     * @return the index of the new active layer to choose, or -1 if no suitable
     *         new layer can be found
     */
    private static int determineNewActiveLayer(int oldActiveLayerIdx) {
        int candidate = oldActiveLayerIdx;
        if (!isValidIndex(candidate)) {
            candidate = layers.size() - 1;
        }

        return candidate;
    }

    /**
     * Remove the layer in question
     *
     * @param view
     *            - View that can be associated with the layer in question
     */
    public static void removeLayer(AbstractView view) {
        LinkedMovieManager.pauseLinkedMovies();

        MoviePanel.getMoviePanelManager().unlinkView(view);
        view.setMoviePanel(null);

        ImageViewerGui.getMoviePanelContainer().removeLayer(view);

        int index = layers.indexOf(view);

        layers.remove(view);
        if (view instanceof JHVJPXView) {
            ((JHVJPXView) view).abolish();
        }

        setActiveView(getLayer(determineNewActiveLayer(index)));
    }

    public static void removeLayer(int idx) {
        removeLayer(getLayer(idx));
    }

    private static void fireLayerAdded(AbstractView view) {
        for (LayersListener ll : layerListeners) {
            ll.layerAdded(view);
        }
    }

    private static void fireActiveLayerChanged(AbstractView view) {
        for (LayersListener ll : layerListeners) {
            ll.activeLayerChanged(view);
        }
    }

    private static final HashSet<LayersListener> layerListeners = new HashSet<LayersListener>();

    public static void addLayersListener(LayersListener layerListener) {
        layerListeners.add(layerListener);
    }

    public static void removeLayersListener(LayersListener layerListener) {
        layerListeners.remove(layerListener);
    }

}
