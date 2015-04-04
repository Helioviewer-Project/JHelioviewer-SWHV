package org.helioviewer.jhv.layers;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.helioviewer.gl3d.model.image.GL3DImageLayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.gui.dialogs.MetaDataDialog;
import org.helioviewer.jhv.io.FileDownloader;
import org.helioviewer.viewmodel.view.ImageInfoView;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.TimedMovieView;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

/**
 * This class is a (redundant) representation of the ViewChain
 * state, and, in addition to this, introduces the concept of an "activeLayer",
 * which is the Layer that is currently operated on by the user/GUI.
 *
 * This class is mainly used by the LayerTable(Model) as an abstraction to the
 * ViewChain.
 *
 * Future development plans still have to show if it is worth to keep this
 * class, or if the abstraction should be avoided and direct access to the
 * viewChain should be used in all GUI classes.
 *
 * @author Malte Nuhn
 */
public class LayersModel {

    private static final LayersModel layersModel = new LayersModel();

    private int activeLayer = -1;
    private final ArrayList<LayersListener> layerListeners = new ArrayList<LayersListener>();

    public LayersModel() {
    }

    /**
     * Return the view associated with the active Layer
     *
     * @return View associated with the active Layer
     */
    public JHVJP2View getActiveView() {
        return getLayer(activeLayer);
    }

    /**
     * @return Index of the currently active Layer
     */
    public int getActiveLayer() {
        return activeLayer;
    }

    /**
     * Set the activeLayer to the Layer associated with the given index
     *
     * @param idx
     *            - index of the layer to be set as active Layer
     */
    public void setActiveLayer(int idx) {
        JHVJP2View view = getLayer(idx);
        if (view == null && idx != -1) {
            return;
        }
        activeLayer = idx;
        fireActiveLayerChanged(view);
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
    public ImmutableDateTime getStartDate(JHVJP2View view) {
        ImmutableDateTime result = null;

        if (view instanceof JHVJPXView) {
            result = ((JHVJPXView) view).getFrameDateTime(0);
        } else {
            result = view.getMetaData().getDateTime();
        }

        return result;
    }

    /**
     * Return the timestamp of the first available image data of the layer in
     * question
     *
     * @param idx
     *            - index of the layer in question
     * @return timestamp of the first available image data, null if no
     *         information available
     */
    public ImmutableDateTime getStartDate(int idx) {
        return getStartDate(getLayer(idx));
    }

    /**
     * Return the timestamp of the first available image data
     *
     * @return timestamp of the first available image data, null if no
     *         information available
     */
    public Date getFirstDate() {
        ImmutableDateTime earliest = null;

        for (int idx = 0; idx < getNumLayers(); idx++) {
            ImmutableDateTime start = getStartDate(idx);
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
     * Return the timestamp of the last available image data of the layer in
     * question
     *
     * @param view
     *            - View that can be associated with the layer in question
     * @return timestamp of the last available image data, null if no
     *         information available
     */
    public ImmutableDateTime getEndDate(JHVJP2View view) {
        ImmutableDateTime result = null;

        if (view instanceof JHVJPXView) {
            JHVJPXView tmv = (JHVJPXView) view;
            int lastFrame = tmv.getMaximumFrameNumber();
            result = tmv.getFrameDateTime(lastFrame);
        } else {
            result = view.getMetaData().getDateTime();
        }
        return result;
    }

    /**
     * Return the timestamp of the last available image data
     *
     * @return timestamp of the last available image data, null if no
     *         information available
     */
    public Date getLastDate() {
        ImmutableDateTime latest = null;

        for (int idx = 0; idx < getNumLayers(); idx++) {
            ImmutableDateTime end = getEndDate(idx);
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
     * Return the timestamp of the last available image data of the layer in
     * question
     *
     * @param idx
     *            - index of the layer in question
     * @return timestamp of the last available image data, null if no
     *         information available
     */
    public ImmutableDateTime getEndDate(int idx) {
        return getEndDate(getLayer(idx));
    }

    /**
     * Find the index of the layer that can be associated with the given view
     *
     * @param view
     *            - View that can be associated with the layer in question
     * @return index of the layer that can be associated with the given view
     */
    public int findView(JHVJP2View view) {
        int idx = -1;
        if (view != null) {
            idx = this.getLayerLevel(view);
        }
        return idx;
    }

    /**
     * Check if the given index is valid, given the current state of the
     * ViewChain
     *
     * @param idx
     *            - index of the layer in question
     * @return true if the index is valid
     */
    private boolean isValidIndex(int idx) {
        if (idx >= 0 && idx < getNumLayers()) {
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
    private int determineNewActiveLayer(int oldActiveLayerIdx) {
        int candidate = oldActiveLayerIdx;
        if (!isValidIndex(candidate)) {
            candidate = getNumLayers() - 1;
        }

        return candidate;
    }

    /**
     * Trigger downloading the layer in question
     *
     * @param view
     *            - View that can be associated with the layer in question
     */
    public void downloadLayer(JHVJP2View view) {
        if (view == null) {
            return;
        }

        Thread downloadThread = new Thread(new Runnable() {
            private ImageInfoView theInfoView;

            @Override
            public void run() {
                downloadFromJPIP(theInfoView);
            }

            public Runnable init(ImageInfoView theInfoView) {
                this.theInfoView = theInfoView;
                return this;
            }
        }.init(view), "DownloadFromJPIPThread");

        downloadThread.start();
    }

    /**
     * Downloads the complete image from the JPIP server.
     *
     * Changes the source of the ImageInfoView afterwards, since a local file is
     * always faster.
     */
    private void downloadFromJPIP(ImageInfoView infoView) {
        FileDownloader fileDownloader = new FileDownloader();
        URI downloadUri = infoView.getDownloadURI();
        URI uri = infoView.getUri();

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
            if (!fileDownloader.get(downloadUri, downloadDestination, "Downloading " + infoView.getName())) {
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
    public void showMetaInfo(JHVJP2View view) {
        MetaDataDialog dialog = new MetaDataDialog();
        dialog.setMetaData(view);
        dialog.showDialog();
    }

    public void addLayer(JHVJP2View view) {
        movieManager.pauseLinkedMovies();

        GL3DImageLayer imageLayer = new GL3DImageLayer("", view, true, true, true);
        view.setImageLayer(imageLayer);
        layers.add(view);

        ImageViewerGui ivg = ImageViewerGui.getSingletonInstance();
        if (view instanceof JHVJPXView) {
            MoviePanel moviePanel = new MoviePanel((JHVJPXView) view);
            setLink(view, true);

            ivg.getMoviePanelContainer().addLayer(view, moviePanel);
        } else {
            MoviePanel moviePanel = new MoviePanel(null);
            ivg.getMoviePanelContainer().addLayer(view, moviePanel);
        }
        int newIndex = layers.size() - 1;
        fireLayerAdded(newIndex);
        setActiveLayer(newIndex);
    }

    /**
     * Remove the layer in question
     *
     * @param view
     *            - View that can be associated with the layer in question
     */
    public void removeLayer(JHVJP2View view) {
        if (view == null)
            return;

        if (view instanceof JHVJPXView) {
            MoviePanel moviePanel = MoviePanel.getMoviePanel((JHVJPXView) view);
            if (moviePanel != null) {
                moviePanel.remove();
            }
        }

        int index = layers.indexOf(view);

        movieManager.pauseLinkedMovies();

        layers.remove(view);
        view.abolish();
        view.removeRenderListener();

        fireLayerRemoved(index);

        int newIndex = determineNewActiveLayer(index);
        setActiveLayer(newIndex);
    }

    public void removeLayer(int idx) {
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
    private void setLink(JHVJP2View view, boolean link) {
        if (view == null) {
            return;
        }

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
    public int getFPS(JHVJP2View view) {
        int result = 0;
        if (view instanceof JHVJPXView) {
            result = Math.round(((JHVJPXView) view).getActualFramerate() * 100) / 100;
        }
        return result;
    }

    private void fireLayerRemoved(int oldIndex) {
        for (LayersListener ll : layerListeners) {
            ll.layerRemoved(oldIndex);
        }
    }

    private void fireLayerAdded(int newIndex) {
        for (LayersListener ll : layerListeners) {
            ll.layerAdded(newIndex);
        }
    }

    private void fireActiveLayerChanged(JHVJP2View view) {
        for (LayersListener ll : layerListeners) {
            ll.activeLayerChanged(view);
        }
    }

    public void addLayersListener(LayersListener layerListener) {
        layerListeners.add(layerListener);
    }

    public void removeLayersListener(LayersListener layerListener) {
        layerListeners.remove(layerListener);
    }

    public LinkedList<TimedMovieView> getLayers() {
        return null;
    }

    private final LinkedMovieManager movieManager = LinkedMovieManager.getSingletonInstance();

    private final ArrayList<JHVJP2View> layers = new ArrayList<JHVJP2View>();

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
     * Returns number of layers
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
     * child, the function returns -1.
     *
     * @param view
     *            View to search for within the stack of layers
     * @return Position of the view within stack
     * @see #moveView
     */
    public int getLayerLevel(JHVJP2View view) {
        return layers.indexOf(view);
    }

}
