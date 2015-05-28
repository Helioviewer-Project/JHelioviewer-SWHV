package org.helioviewer.jhv.layers;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import javax.swing.JOptionPane;

import org.helioviewer.base.datetime.ImmutableDateTime;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.gui.dialogs.MetaDataDialog;
import org.helioviewer.jhv.io.FileDownloader;
import org.helioviewer.jhv.renderable.components.RenderableImageLayer;
import org.helioviewer.viewmodel.view.AbstractView;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;

public class LayersModel {

    private static int activeLayer = -1;
    private static final ArrayList<AbstractView> layers = new ArrayList<AbstractView>();

    private static final HashSet<LayersListener> layerListeners = new HashSet<LayersListener>();

    /**
     * Return the view associated with the active Layer
     *
     * @return View associated with the active Layer
     */
    public static AbstractView getActiveView() {
        return getLayer(activeLayer);
    }

    /**
     * @return Index of the currently active Layer
     */
    public static int getActiveLayer() {
        return activeLayer;
    }

    /**
     * Set the activeLayer to the Layer associated with the given index
     *
     * @param idx
     *            - index of the layer to be set as active Layer
     */
    public static void setActiveLayer(int idx) {
        AbstractView view = getLayer(idx);
        if (view == null && idx != -1) {
            return;
        }
        activeLayer = idx;
        fireActiveLayerChanged(view);
    }

    public static void setActiveLayer(AbstractView view) {
        setActiveLayer(findView(view));
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

    /**
     * Find the index of the layer that can be associated with the given view
     *
     * @param view
     *            - View that can be associated with the layer in question
     * @return index of the layer that can be associated with the given view
     */
    public static int findView(AbstractView view) {
        return layers.indexOf(view);
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
     * Calulate a new activeLayer after the old Layer has been deleted
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
     * Trigger downloading the layer in question
     *
     * @param view
     *            - View that can be associated with the layer in question
     */
    public static void downloadLayer(JHVJP2View view) {
        Thread downloadThread = new Thread(new Runnable() {
            private JHVJP2View theView;

            @Override
            public void run() {
                downloadFromJPIP(theView);
            }

            public Runnable init(JHVJP2View theView) {
                this.theView = theView;
                return this;
            }
        }.init(view), "DownloadFromJPIPThread");
        downloadThread.start();
    }

    /**
     * Downloads the complete image from the JPIP server.
     *
     * Changes the source of the View afterwards, since a local file is always
     * faster.
     */
    private static void downloadFromJPIP(JHVJP2View v) {
        FileDownloader fileDownloader = new FileDownloader();
        URI downloadUri = v.getDownloadURI();
        URI uri = v.getUri();

        // the http server to download the file from is unknown
        if (downloadUri.equals(uri) && !downloadUri.toString().contains("delphi.nascom.nasa.gov")) {
            String inputValue = JOptionPane.showInputDialog("To download this file, please specify a concurrent HTTP server address to the JPIP server: ", uri);
            if (inputValue != null) {
                try {
                    downloadUri = new URI(inputValue);
                } catch (URISyntaxException e) {
                }
            }
        }

        File downloadDestination = fileDownloader.getDefaultDownloadLocation(uri);
        try {
            if (!fileDownloader.get(downloadUri, downloadDestination, "Downloading " + v.getName())) {
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    /**
     * Trigger showing a dialog displaying the meta data of the layer in
     * question.
     *
     * @param view
     *            - View that can be associated with the layer in question
     *
     * @see org.helioviewer.jhv.gui.dialogs.MetaDataDialog
     */
    public static void showMetaInfo(AbstractView view) {
        MetaDataDialog dialog = new MetaDataDialog();
        dialog.setMetaData(view);
        dialog.showDialog();
    }

    public static void addLayer(AbstractView view) {
        if (view == null)
            return;

        // needed for proper linked movies (tbd)
        LinkedMovieManager.getSingletonInstance().pauseLinkedMovies();

        RenderableImageLayer imageLayer = new RenderableImageLayer(view);
        view.setImageLayer(imageLayer);
        layers.add(view);

        MoviePanel moviePanel;
        if (view instanceof JHVJPXView) {
            moviePanel = new MoviePanel((JHVJPXView) view);
            setLink(view, true);
        } else {
            moviePanel = new MoviePanel(null);
        }
        ImageViewerGui.getMoviePanelContainer().addLayer(view, moviePanel);

        fireLayerAdded(view);
        setActiveLayer(layers.size() - 1);
    }

    public static void addView(AbstractView view) {
        if (view == null)
            return;

        EventQueue.invokeLater(new Runnable() {
            private AbstractView theView;

            @Override
            public void run() {
                addLayer(theView);
            }

            public Runnable init(AbstractView theView) {
                this.theView = theView;
                return this;
            }
        }.init(view));
    }

    /**
     * Remove the layer in question
     *
     * @param view
     *            - View that can be associated with the layer in question
     */
    public static void removeLayer(AbstractView view) {
        if (view instanceof JHVJPXView) {
            MoviePanel moviePanel = MoviePanel.getMoviePanel((JHVJPXView) view);
            if (moviePanel != null) {
                ((JHVJPXView) view).pauseMovie();
                moviePanel.remove();
            }
        }
        ImageViewerGui.getMoviePanelContainer().removeLayer(view);

        int index = layers.indexOf(view);

        layers.remove(view);
        if (view instanceof JHVJPXView) {
            ((JHVJPXView) view).abolish();
        }

        int newIndex = determineNewActiveLayer(index);
        setActiveLayer(newIndex);
    }

    public static void removeLayer(int idx) {
        removeLayer(getLayer(idx));
    }

    /**
     * Set the link-state of the layer in question
     *
     * @param view
     *            - View that can be associated with the layer in question
     * @param link
     *            - true if the layer in question should be linked
     */
    private static void setLink(AbstractView view, boolean link) {
        if (view instanceof JHVJPXView) {
            MoviePanel moviePanel = MoviePanel.getMoviePanel((JHVJPXView) view);
            if (moviePanel != null) {
                moviePanel.setMovieLink(link);
            }
        }
    }

    /**
     * Return the current framerate for the layer in question
     *
     * @param view
     *            - View that can be associated with the layer in question
     * @return the current framerate or 0 if the movie is not playing, or if an
     *         error occurs
     */
    public static int getFPS(JHVJP2View view) {
        int result = 0;
        if (view instanceof JHVJPXView) {
            result = Math.round(((JHVJPXView) view).getActualFramerate() * 100) / 100;
        }
        return result;
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

    public static void addLayersListener(LayersListener layerListener) {
        layerListeners.add(layerListener);
    }

    public static void removeLayersListener(LayersListener layerListener) {
        layerListeners.remove(layerListener);
    }

    /**
     * Returns the view at a given position within the stack of layers.
     *
     * @param index
     *            Position within the stack of layers
     * @return View at given position
     */
    public static AbstractView getLayer(int index) {
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

}
