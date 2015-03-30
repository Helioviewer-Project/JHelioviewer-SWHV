package org.helioviewer.jhv.layers;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.JOptionPane;
import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.gui.dialogs.MetaDataDialog;
import org.helioviewer.jhv.io.FileDownloader;
import org.helioviewer.viewmodel.view.ImageInfoView;
import org.helioviewer.viewmodel.view.LayeredView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

/**
 * This class is a (redundant) representation of the LayeredView + ViewChain
 * state, and, in addition to this, introduces the concept of an "activeLayer",
 * which is the Layer that is currently operated on by the user/GUI.
 *
 * This class is mainly used by the LayerTable(Model) as an abstraction to the
 * ViewChain.
 *
 * Future development plans still have to show if it is worth to keep this
 * class, or if the abstraction should be avoided and direct access to the
 * viewChain/layeredView should be used in all GUI classes.
 *
 * @author Malte Nuhn
 */
public class LayersModel extends AbstractTableModel {

    private static final LayersModel layersModel = new LayersModel();

    private int activeLayer = -1;
    private final ArrayList<LayersListener> layerListeners = new ArrayList<LayersListener>();

    private static final LayeredView layeredView = new LayeredView();
    private static final ArrayList<JHVJP2View> views = new ArrayList<JHVJP2View>();

    /**
     * Method returns the sole instance of this class.
     *
     * @return the only instance of this class.
     * */
    public static LayersModel getSingletonInstance() {
        if (!EventQueue.isDispatchThread()) {
            System.out.println(">>> You have been naughty: " + Thread.currentThread().getName());
            Thread.dumpStack();
            System.exit(1);
        }
        return layersModel;
    }

    private LayersModel() {
    }

    public LayeredView getLayeredView() {
        return layeredView;
    }

    /* <LayerTableModel> */

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRowCount() {
        return views.size();
    }

    /**
     * {@inheritDoc} Hardcoded value of columns. This value is dependent on the
     * actual design of the LayerTable
     */
    @Override
    public int getColumnCount() {
        return 4;
    }

    /**
     * Return the LayerDescriptor for the given row of the table, regardless
     * which column is requested.
     */
    @Override
    public Object getValueAt(int idx, int col) {
        if (idx >= 0 && idx < views.size()) {
            return layeredView.getLayerDescriptor(views.get(idx));
        }
        return null;
    }

    private void updateData() {
        views.clear();
        for (int i = layeredView.getNumLayers() - 1; i >= 0; i--) {
            views.add(layeredView.getLayer(i));
        }
    }

    /* </LayerTableModel> */

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
     * @param idx
     *            - Index of the layer to be retrieved
     * @return View associated with the given index
     */
    public JHVJP2View getLayer(int idx) {
        idx = invertIndex(idx);
        if (idx >= 0 && idx < getNumLayers()) {
            return layeredView.getLayer(idx);
        }
        return null;
    }

    /**
     * Set the activeLayer to the Layer that can be associated to the given
     * view, do nothing if the view cannot be associated with any layer
     *
     * @param view
     */
    public void setActiveLayer(JHVJP2View view) {
        int i = findView(view);
        setActiveLayer(i);
    }

    /**
     * Set the activeLayer to the Layer associated with the given index
     *
     * @param idx
     *            - index of the layer to be set as active Layer
     */
    public void setActiveLayer(int idx) {
        View view = getLayer(idx);
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
            idx = layeredView.getLayerLevel(view);
        }
        return invertIndex(idx);
    }

    /**
     * Important internal method to convert between LayersModel indexing and
     * LayeredView indexing. Calling it twice should form the identity
     * operation.
     *
     * LayersModel indices go from 0 .. (LayerCount - 1), with 0 being the
     * uppermost layer
     *
     * whereas
     *
     * LayeredView indies go from (LayerCount - 1) .. 0, with 0 being the layer
     * at the bottom.
     *
     * @param idx
     *            to be converted from LayersModel to LayeredView or the other
     *            direction.
     * @return the inverted index
     */
    private int invertIndex(int idx) {
        int num = this.getNumLayers();
        // invert indices
        if (idx >= 0 && num > 0) {
            idx = num - 1 - idx;
        }
        return idx;
    }

    /**
     * Important internal method to convert between LayersModel indexing and
     * LayeredView indexing.
     *
     * Since this index transformation involves the number of layers, this
     * transformation has to pay respect to situation where the number of layers
     * has changed.
     *
     * @param idx
     *            to be converted from LayersModel to LayeredView or the other
     *            direction after a layer has been deleted
     * @return inverted index
     */
    private int invertIndexDeleted(int idx) {
        int num = this.getNumLayers();
        if (idx >= 0) {
            idx = num - idx;
        }
        return idx;
    }

    /**
     * Return the number of layers currently available
     *
     * @return number of layers
     */
    public int getNumLayers() {
        return layeredView.getNumLayers();
    }

    /**
     * Change the visibility of the layer in question, and automatically
     * (un)link + play/pause the layer
     *
     * @param view
     *            - View that can be associated with the layer in question
     */
    public void toggleVisibility(JHVJP2View view) {
        layeredView.toggleVisibility(view);

        ImageViewerGui.getSingletonInstance().getMoviePanelContainer().layerVisibilityChanged(view);
        int idx = findView(view);
        fireTableRowsUpdated(idx, idx);

        boolean visible = layeredView.isVisible(view);
        setLink(view, visible);

        if (!visible) {
            this.setPlaying(view, false);
        }
    }

    public void toggleVisibility(int idx) {
        toggleVisibility(getLayer(idx));
    }

    /**
     * Get the visibility of the layer in question
     *
     * @param view
     *            - View that can be associated with the layer in question
     *
     * @return true if the layer is visible
     */
    public boolean isVisible(JHVJP2View view) {
        return layeredView.isVisible(view);
    }

    /**
     * Check if the given index is valid, given the current state of the
     * LayeredView/ViewChain
     *
     * @param idx
     *            - index of the layer in question
     * @return true if the index is valid
     */
    private boolean isValidIndex(int idx) {
        if (idx >= 0 && idx < this.getNumLayers()) {
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
            candidate = this.getNumLayers() - 1;
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
        if (view == null) {
            return;
        }

        MetaDataDialog dialog = new MetaDataDialog();
        dialog.setMetaData(view);
        dialog.showDialog();
    }


    public void addLayer(JHVJP2View view) {
        if (view == null) {
            return;
        }

        int newIndex = invertIndex(layeredView.addLayer(view));
        fireLayerAdded(newIndex);

        ImageViewerGui ivg = ImageViewerGui.getSingletonInstance();
        // If MoviewView, add MoviePanel
        if (view instanceof JHVJPXView) {
            MoviePanel moviePanel = new MoviePanel((JHVJPXView) view);
            if (isTimed(view)) {
                setLink(view, true);
            }
            ivg.getMoviePanelContainer().addLayer(view, moviePanel);
        } else {
            MoviePanel moviePanel = new MoviePanel(null);
            ivg.getMoviePanelContainer().addLayer(view, moviePanel);
        }

        setActiveLayer(newIndex);

        updateData();
        fireTableRowsInserted(newIndex, newIndex);
    }

    /**
     * Remove the layer in question
     *
     * @param view
     *            - View that can be associated with the layer in question
     */
    public void removeLayer(JHVJP2View view) {
        if (view == null) {
            return;
        }

        if (view instanceof JHVJPXView) {
            MoviePanel moviePanel = MoviePanel.getMoviePanel((JHVJPXView) view);
            if (moviePanel != null) {
                moviePanel.remove();
            }
        }

        int oldIndex = invertIndexDeleted(layeredView.removeLayer(view));
        fireLayerRemoved(oldIndex);

        int newIndex = determineNewActiveLayer(oldIndex);
        setActiveLayer(newIndex);

        updateData();
        fireTableRowsDeleted(oldIndex, oldIndex);
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
     * Set the play-state of the layer in question
     *
     * @param view
     *            - View that can be associated with the layer in question
     * @param play
     *            - true if the layer in question should play
     */
    public void setPlaying(JHVJP2View view, boolean play) {
        if (view == null) {
            return;
        }

        if (view instanceof JHVJPXView) {
            JHVJPXView timedMovieView = (JHVJPXView) view;
            if (play) {
                timedMovieView.playMovie();
            } else {
                timedMovieView.pauseMovie();
            }
        }
    }

    /**
     * Check whether the layer in question has timing information
     *
     * @param idx
     *            - index of the layer in question
     * @return true if the layer in question has timing information
     */
    public boolean isTimed(int idx) {
        return isTimed(getLayer(idx));
    }

    /**
     * Check whether the layer in question has timing information
     *
     * @param view
     *            - View that can be associated with the layer in question
     * @return true if the layer in question has timing information
     */
    public boolean isTimed(JHVJP2View view) {
        return layeredView.getLayerDescriptor(view).isTimed;
    }

    /**
     * Move the layer in question up
     *
     * @param view
     *            - View that can be associated with the layer in question
     */
    public void moveLayerUp(JHVJP2View view) {
        if (view == null) {
            return;
        }

        // Operates on the (inverted) LayeredView indices
        int level = layeredView.getLayerLevel(view);
        if (level < layeredView.getNumLayers() - 1) {
            level++;
        }

        layeredView.moveView(view, level);
        updateData();
        this.setActiveLayer(invertIndex(level));
    }

    public void moveLayerUp(int idx) {
        moveLayerUp(getLayer(idx));
    }

    /**
     * Move the layer in question down
     *
     * @param view
     *            - View that can be associated with the layer in question
     */
    public void moveLayerDown(JHVJP2View view) {
        if (view == null) {
            return;
        }

        // Operates on the (inverted) LayeredView indices
        int level = layeredView.getLayerLevel(view);
        if (level > 0) {
            level--;
        }

        layeredView.moveView(view, level);
        updateData();
        this.setActiveLayer(invertIndex(level));
    }

    public void moveLayerDown(int idx) {
        moveLayerDown(getLayer(idx));
    }

    /**
     * Check whether the layer in question is currently playing
     *
     * @param view
     *            - View that can be associated with the layer in question
     * @return true if the layer in question is currently playing
     */
    public boolean isPlaying(JHVJP2View view) {
        if (view instanceof JHVJPXView) {
            return ((JHVJPXView) view).isMoviePlaying();
        } else {
            return false;
        }
    }

    /**
     * Return the current framerate for the layer in question
     *
     * @param view
     *            - View that can be associated with the layer in question
     * @return the current framerate or 0 if the movie is not playing, or if
     *         an error occurs
     */
    public int getFPS(JHVJP2View view) {
        int result = 0;
        if (view instanceof JHVJPXView) {
            result = (int) (Math.round(((JHVJPXView) view).getActualFramerate() * 100) / 100);
        }
        return result;
    }

    /**
     * Check whether the layer in question is a Remote View
     *
     * @param view
     *            - View that can be associated with the layer in question
     * @return true if the layer in question is a remote view
     */
    public boolean isRemote(JHVJP2View view) {
        if (view != null) {
            return view.isRemote();
        } else {
            return false;
        }
    }

    /**
     * Check whether the layer in question is connected to a JPIP server
     *
     * @param view
     *            - View that can be associated with the layer in question
     * @return true if the layer is connected to a JPIP server
     */
    public boolean isConnectedToJPIP(JHVJP2View view) {
        if (view != null) {
            return view.isConnectedToJPIP();
        } else {
            return false;
        }
    }

    /**
     * Return a representation of the layer in question
     *
     * @param view
     *            - View that can be associated with the layer in question
     * @return LayerDescriptor of the current state of the layer in question
     */
    public LayerDescriptor getDescriptor(JHVJP2View view) {
        return layeredView.getLayerDescriptor(view);
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

    private void fireActiveLayerChanged(View view) {
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

}
