package org.helioviewer.jhv.layers;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import javax.swing.JOptionPane;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.UIViewListener;
import org.helioviewer.jhv.gui.UIViewListenerDistributor;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.gui.components.layerTable.LayerTableModel;
import org.helioviewer.jhv.gui.components.statusplugins.PositionStatusPanel;
import org.helioviewer.jhv.gui.components.statusplugins.ZoomStatusPanel;
import org.helioviewer.jhv.gui.dialogs.MetaDataDialog;
import org.helioviewer.jhv.io.APIRequestManager;
import org.helioviewer.jhv.io.FileDownloader;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.ChangedReason;
import org.helioviewer.viewmodel.changeevent.LayerChangedReason;
import org.helioviewer.viewmodel.changeevent.LayerChangedReason.LayerChangeType;
import org.helioviewer.viewmodel.changeevent.RegionChangedReason;
import org.helioviewer.viewmodel.changeevent.ViewportChangedReason;
import org.helioviewer.viewmodel.io.APIResponse;
import org.helioviewer.viewmodel.io.APIResponseDump;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.GL3DLayeredView;
import org.helioviewer.viewmodel.view.ImageInfoView;
import org.helioviewer.viewmodel.view.LayeredView;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

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
public class LayersModel implements UIViewListener {

    private static final LayersModel layersModel = new LayersModel();

    private int activeLayer = -1;
    private final ArrayList<LayersListener> layerListeners = new ArrayList<LayersListener>();

    private final GL3DLayeredView layeredView;

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
        layeredView = new GL3DLayeredView();
        UIViewListenerDistributor.getSingletonInstance().addViewListener(this);
    }

    public LayeredView getLayeredView() {
        return layeredView;
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
        int i = this.findView(view);
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
     * Return a String containing the current timestamp of the given layer,
     * return an empty string if no timing information is available
     *
     * @param idx
     *            - Index of the layer in question
     * @return String representation of the timestamp, empty String if no timing
     *         information is available
     */
    public String getCurrentFrameTimestampString(int idx) {
        JHVJP2View view = this.getLayer(idx);
        return getCurrentFrameTimestampString(view);
    }

    /**
     * Return a String containing the current timestamp of the given layer,
     * return an empty string if no timing information is available
     *
     * @param view
     *            - View that can be associated with the layer in question
     * @return String representation of the timestamp, empty String if no timing
     *         information is available
     */
    public String getCurrentFrameTimestampString(JHVJP2View view) {
        ImmutableDateTime dt = getCurrentFrameTimestamp(view);
        if (dt != null) {
            return dt.getCachedDate();
        }
        return "N/A";
    }

    /**
     * Return the current timestamp of the given layer, return an empty string
     * if no timing information is available
     *
     * @param idx
     *            - Index of the layer in question
     * @return timestamp, null if no timing information is available
     */
    public ImmutableDateTime getCurrentFrameTimestamp(int idx) {
        JHVJP2View view = this.getLayer(idx);
        return getCurrentFrameTimestamp(view);
    }

    /**
     * Return the current timestamp of the given layer, return an empty string
     * if no timing information is available
     *
     * @param view
     *            - View that can be associated with the layer in question
     * @return timestamp, null if no timing information is available
     */
    public ImmutableDateTime getCurrentFrameTimestamp(JHVJP2View view) {
        if (view != null) {
            // null for PixelBasedMetaData
            return view.getMetaData().getDateTime();
        }
        return null;
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

        if (view instanceof JHVJPXView && view != null) {
            JHVJPXView tmv = (JHVJPXView) view;
            result = tmv.getFrameDateTime(0);
        } else {
            result = getCurrentFrameTimestamp(view);
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
        JHVJP2View view = this.getLayer(idx);
        return this.getStartDate(view);
    }

    public Interval<Date> getFrameInterval() {
        return new Interval<Date>(getFirstDate(), getLastDate());
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

        if (view instanceof JHVJPXView && view != null) {
            JHVJPXView tmv = (JHVJPXView) view;
            int lastFrame = tmv.getMaximumFrameNumber();
            result = tmv.getFrameDateTime(lastFrame);
        } else {
            result = getCurrentFrameTimestamp(view);
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
        JHVJP2View view = this.getLayer(idx);
        return this.getEndDate(view);
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
     * @param visible
     *            - the new visibility state
     */
    public void setVisibleLink(JHVJP2View view, boolean visible) {
        this.setVisible(view, visible);
        this.setLink(view, visible);

        if (!visible) {
            this.setPlaying(view, false);
        }
    }

    /**
     * Change the visibility of the layer in question, and automatically
     * (un)link + play/pause the layer
     *
     * @param idx
     *            - index of the layer in question
     * @param visible
     *            - the new visibility state
     */
    public void setVisibleLink(int idx, boolean visible) {
        JHVJP2View view = this.getLayer(idx);
        this.setVisibleLink(view, visible);
    }

    /**
     * Change the visibility of the layer in question
     *
     * @param view
     *            - View that can be associated with the layer in question
     * @param visible
     *            - the new visibility state
     */
    public void setVisible(JHVJP2View view, boolean visible) {
        if (layeredView.isVisible(view) != visible) {
            layeredView.toggleVisibility(view);
        }
    }

    /**
     * Change the visibility of the layer in question
     *
     * @param idx
     *            - index of the layer in question
     * @param visible
     *            - the new visibility state
     */
    public void setVisible(int idx, boolean visible) {
        setVisible(getLayer(idx), visible);
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
     * Get the visibility of the layer in question
     *
     * @param idx
     *            - index of the layer in question
     * @return true if the layer is visible
     */
    public boolean isVisible(int idx) {
        return isVisible(getLayer(idx));
    }

    /**
     * Get the name of the layer in question
     *
     * @param view
     *            - View that can be associated with the layer in question
     * @return name of the layer, the views default String representation if no
     *         name is available
     */
    public String getName(JHVJP2View view) {
        if (view == null) {
            return null;
        }
        return view.toString();
    }

    /**
     * Get the name of the layer in question
     *
     * @param idx
     *            - index of the layer in question
     * @return name of the layer, the views default String representation if no
     *         name is available
     */
    public String getName(int idx) {
        JHVJP2View view = this.getLayer(idx);
        return this.getName(view);
    }

    private void handleLayerChanges(View sender, ChangeEvent aEvent) {
        LayerChangedReason layerReason = aEvent.getLastChangedReasonByType(LayerChangedReason.class);

        // if layers were changed, perform same operations on GUI
        if (layerReason != null && !layerReason.getProcessed()) {
            LayerChangeType type = layerReason.getLayerChangeType();
            // If layer was deleted, delete corresponding panel
            if (type == LayerChangedReason.LayerChangeType.LAYER_ADDED) {
                layerReason.setProcessed(true);
                JHVJP2View view = (JHVJP2View) layerReason.getSubView();
                int newIndex = findView(view);
                if (newIndex != -1) {
                    this.setActiveLayer(newIndex);
                    this.fireLayerAdded(newIndex);
                }
            } else if (type == LayerChangedReason.LayerChangeType.LAYER_REMOVED) {
                layerReason.setProcessed(true);
                int oldIndex = this.invertIndexDeleted(layerReason.getLayerIndex());
                this.fireLayerRemoved(layerReason.getView(), oldIndex);
                int newIndex = determineNewActiveLayer(oldIndex);
                this.setActiveLayer(newIndex);
            } else if (type == LayerChangedReason.LayerChangeType.LAYER_VISIBILITY) {
                layerReason.setProcessed(true);
                JHVJP2View view = (JHVJP2View) layerReason.getSubView();
                int idx = findView(view);
                if (idx != -1) {
                    ImageViewerGui.getSingletonInstance().getMoviePanelContainer().layerChanged(idx);
                    LayerTableModel.getSingletonInstance().layerChanged(idx);
                }
            }
        }
    }

    private void handleViewportPositionChanges(View sender, ChangeEvent aEvent) {
        ChangedReason reason1 = aEvent.getLastChangedReasonByType(RegionChangedReason.class);
        ChangedReason reason2 = aEvent.getLastChangedReasonByType(ViewportChangedReason.class);

        if (reason1 != null || reason2 != null) {
            // PositionStatusPanel.getSingletonInstance().updatePosition();
            ZoomStatusPanel.getSingletonInstance().updateZoomLevel(getActiveView());
        }
    }

    /**
     * View changed handler.
     *
     * Internally forwards (an abstraction) of the events to the LayersListener
     *
     */
    @Override
    public void UIviewChanged(View sender, ChangeEvent aEvent) {
        handleLayerChanges(sender, aEvent);
        handleViewportPositionChanges(sender, aEvent);
    }

    /**
     * Check if the given index is valid, given the current state of the
     * LayeredView/ViewChain
     *
     * @param idx
     *            - index of the layer in question
     * @return true if the index is valid
     */
    public boolean isValidIndex(int idx) {
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
     * @param idx
     *            - index of the layer in question
     */
    public void downloadLayer(int idx) {
        downloadLayer(layeredView.getLayer(idx));
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
     * @param idx
     *            - index of the layer in question
     */
    public void showMetaInfo(int idx) {
        JHVJP2View view = getLayer(idx);
        showMetaInfo(view);
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

    /**
     * Remove the layer in question
     *
     * @param idx
     *            - index of the layer in question
     */
    public void removeLayer(int idx) {
        idx = invertIndex(idx);
        layeredView.removeLayer(idx);
    }

    /**
     * Remove the layer in question
     *
     * @param view
     *            - View that can be associated with the layer in question
     */
    public void removeLayer(JHVJP2View view) {
        int index = this.findView(view);
        removeLayer(index);
    }

    /**
     * Set the link-state of the layer in question
     *
     * @param view
     *            - View that can be associated with the layer in question
     * @param link
     *            - true if the layer in question should be linked
     */
    public void setLink(JHVJP2View view, boolean link) {
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
     * Set the link-state of the layer in question
     *
     * @param idx
     *            - index of the layer in question
     * @param link
     *            - true if the layer in question should be linked
     */
    public void setLink(int idx, boolean link) {
        JHVJP2View view = this.getLayer(idx);
        this.setLink(view, link);
    }

    /**
     * Set the play-state of the layer in question
     *
     * @param idx
     *            - index of the layer in question
     * @param play
     *            - true if the layer in question should play
     */
    public void setPlaying(int idx, boolean play) {
        JHVJP2View view = this.getLayer(idx);
        this.setPlaying(view, play);
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
        return isTimed(this.getLayer(idx));
    }

    /**
     * Check whether the layer in question has timing information
     *
     * @param view
     *            - View that can be associated with the layer in question
     * @return true if the layer in question has timing information
     */
    public boolean isTimed(JHVJP2View view) {
        if (getCurrentFrameTimestamp(view) != null) {
            return true;
        }
        return false;
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
        this.setActiveLayer(invertIndex(level));
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
        this.setActiveLayer(invertIndex(level));
    }

    /**
     * Check whether the layer in question is currently playing
     *
     * @param idx
     *            - index of the layer in question
     * @return true if the layer in question is currently playing
     */
    public boolean isPlaying(int idx) {
        JHVJP2View view = getLayer(idx);
        return isPlaying(view);
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
     * @param idx
     *            - index of the layer in question
     * @return the current framerate or 0 if the movie is not playing, or if
     *         an error occurs
     */
    public int getFPS(int idx) {
        JHVJP2View view = getLayer(idx);
        return getFPS(view);
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
     * Check whether the layer in question is a Remote View
     *
     * @param idx
     *            - index of the layer in question
     * @return true if the layer in question is a remote view
     */
    public boolean isRemote(int idx) {
        JHVJP2View view = getLayer(idx);
        return isRemote(view);
    }

    /**
     * Check whether the layer in question is connected to a JPIP server
     *
     * @param idx
     *            - index of the layer in question
     * @return true if the layer is connected to a JPIP server
     */
    public boolean isConnectedToJPIP(int idx) {
        JHVJP2View view = getLayer(idx);
        return isConnectedToJPIP(view);
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
     * @param idx
     *            - index of the layer in question
     * @return LayerDescriptor of the current state of the layer in question
     */
    public LayerDescriptor getDescriptor(int idx) {
        JHVJP2View view = this.getLayer(idx);
        return getDescriptor(view);
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

    /**
     * Notify all LayersListeners
     */
    private void fireLayerRemoved(final View oldView, final int oldIndex) {
        for (LayersListener ll : layerListeners) {
            ll.layerRemoved(oldView, oldIndex);
        }
    }

    /**
     * Notify all LayersListeners
     */
    private void fireLayerAdded(final int newIndex) {
        for (LayersListener ll : layerListeners) {
            ll.layerAdded(newIndex);
        }
    }

    /**
     * Notify all LayersListeners
     */
    private void fireActiveLayerChanged(View view) {
        for (LayersListener ll : layerListeners) {
            ll.activeLayerChanged(view);
        }
    }

    /**
     * Notify all LayersListeners
     */
    public void addLayersListener(LayersListener layerListener) {
        layerListeners.add(layerListener);
    }

    /**
     * Remove LayersListener
     *
     * * @author Carlos Martin
     */
    public void removeLayersListener(LayersListener layerListener) {
        layerListeners.remove(layerListener);
    }

    /**
     * Return a XML representation of the current layers. This also includes the
     * filter state for each layer.
     *
     * @see org.helioviewer.viewmodel.filter.Filter#getState
     * @param tab
     *            - String to be prepended to each line of the xml
     *            representation
     * @return the layers' xml representation as string
     */
    public String getXMLRepresentation(String tab) {
        int layers = getNumLayers();
        if (layers == 0) {
            return "";
        }

        StringBuffer xml = new StringBuffer();
        xml.append(tab).append("<layers>\n");
        // add tab
        tab = tab + "\t";

        // store region
        RegionView regionView = ImageViewerGui.getSingletonInstance().getMainView().getAdapter(RegionView.class);
        Region region = regionView.getRegion();
        String regionStr = String.format(Locale.ENGLISH, "<region x=\"%.4f\" y=\"%.4f\" width=\"%.4f\" height=\"%.4f\"/>\n", region.getCornerX(), region.getCornerY(), region.getWidth(), region.getHeight());
        xml.append(tab).append(regionStr);

        // store layers
        for (int i = 0; i < layers; i++) {
            View currentView = LayersModel.getSingletonInstance().getLayer(i);
            if (currentView != null) {
                xml.append(tab).append("<layer id=\"").append(i).append("\">\n");

                ImageInfoView currentImageInfoView = currentView.getAdapter(ImageInfoView.class);

                // add tab
                tab = tab + "\t";
                // TODO Use a proper encoding function - not "replace X->Y"
                xml.append(tab).append("<uri>").append(currentImageInfoView.getUri().toString().replaceAll("&", "&amp;")).append("</uri>\n");
                xml.append(tab).append("<downloaduri>").append(currentImageInfoView.getDownloadURI().toString().replaceAll("&", "&amp;")).append("</downloaduri>\n");

                // check if we got any api response
                APIResponse apiResponse = APIResponseDump.getSingletonInstance().getResponse(currentImageInfoView.getUri(), true);
                if (apiResponse != null) {
                    String xmlApiResponse = apiResponse.getXMLRepresentation();
                    xml.append(tab).append("<apiresponse>\n").append(tab).append("\t").append(xmlApiResponse).append("\n").append(tab).append("</apiresponse>\n");
                }

                // remove last tab
                tab = tab.substring(0, tab.length() - 1);
                xml.append(tab).append("</layer>\n");
            }
        }

        // remove last tab
        tab = tab.substring(0, tab.length() - 1);
        xml.append(tab).append("</layers>");

        return xml.toString();
    }

    /**
     * Restore the JHV state from the given file. This will overwrite the
     * current JHV state without further notice!
     *
     * @param stateURL
     *            - URL to read the JHV state from
     */
    public void loadState(URL stateURL) {
        try {
            InputSource stateInputSource = new InputSource(new InputStreamReader(stateURL.openStream()));
            // create new parser for this inputsource
            StateParser stateParser = new StateParser(stateInputSource);
            // finally tell the parser to setup the viewchain
            stateParser.setupLayers();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class StateParser extends DefaultHandler {

        /**
         * Buffer needed for reading data in-between XML tags
         */
        private StringBuffer stringBuffer = new StringBuffer("");

        /**
         * Temporary variable storing all information about the filter currently
         * being parsed
         */
        private FilterState tmpFilterSetting = null;
        /**
         * Temporary variable storing all information about the layer currently
         * being parsed
         */
        private LayerState tmpLayerSetting = null;

        /**
         * Variable storing all information about the state currently being
         * parsed
         */
        private final FullState fullSetting = new FullState();

        /**
         * Flag showing, if the parser should "copy" the raw XML data currently
         * being read in order to feed it to the json parser later on
         */
        private boolean apiResponseMode = false;

        /**
         * Default Constructor
         *
         * @param xmlSource
         */
        public StateParser(InputSource xmlSource) {
            try {
                XMLReader parser = XMLReaderFactory.createXMLReader("com.sun.org.apache.xerces.internal.parsers.SAXParser");
                parser.setContentHandler(this);
                parser.parse(xmlSource);
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void characters(char ch[], int start, int length) throws SAXException {
            stringBuffer.append(new String(ch, start, length));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void startElement(String namespaceURI, String localName, String qualifiedName, Attributes atts) throws SAXException {
            String tagName = localName.toLowerCase();

            if (tagName.equals("apiresponse")) {
                apiResponseMode = true;

                // if this is enclosed in apiresponse-tags, store the "raw XML"
                // in the stringBuffer
            } else if (apiResponseMode == true) {
                stringBuffer.append("<" + localName);
                for (int i = 0; i < atts.getLength(); i++) {
                    stringBuffer.append(" ");
                    stringBuffer.append(atts.getLocalName(i));
                    stringBuffer.append("=\"");
                    stringBuffer.append(atts.getValue(i));
                    stringBuffer.append("\"");
                }
                stringBuffer.append(">");
                // skip all other tags
                return;
            }

            stringBuffer = new StringBuffer("");

            if (tagName.equals("region")) {
                try {
                    // the attribute names are all case sensitive!
                    fullSetting.regionViewState.regionX = Double.parseDouble(atts.getValue("x"));
                    fullSetting.regionViewState.regionY = Double.parseDouble(atts.getValue("y"));
                    fullSetting.regionViewState.regionWidth = Double.parseDouble(atts.getValue("width"));
                    fullSetting.regionViewState.regionHeight = Double.parseDouble(atts.getValue("height"));
                } catch (Exception e) {
                    Log.fatal(">> LayersModel.StateParser.startElement() > Error parsing region data");
                    fullSetting.regionViewState = null;
                }
            } else if (tagName.equals("layer")) {
                tmpLayerSetting = new LayerState();

                Log.info("new layer setting ");
                // try to read the "id" attribute
                int idx_layer_id = atts.getIndex("id");

                if (idx_layer_id != -1) {
                    String str_layer_id = atts.getValue(idx_layer_id);
                    tmpLayerSetting.id = Integer.parseInt(str_layer_id);
                }
            } else if (tagName.equals("filter")) {
                tmpFilterSetting = new FilterState();
                int idx_filter_name = atts.getIndex("name");
                if (idx_filter_name != -1) {
                    tmpFilterSetting.name = atts.getValue(idx_filter_name);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void endElement(String namespaceURI, String localName, String qualifiedName) throws SAXException {
            String tagName = localName.toLowerCase();

            if (tagName.equals("apiresponse")) {
                if (tmpLayerSetting != null) {
                    tmpLayerSetting.apiResponse = stringBuffer.toString();
                }
                apiResponseMode = false;
            }

            // if this is enclosed in apiresponse-tags, store the "raw XML" in
            // the stringBuffer
            if (apiResponseMode) {
                stringBuffer.append("</" + localName + ">");
                return;
            }

            if (tagName.equals("filter")) {
                if (tmpFilterSetting != null) {
                    tmpFilterSetting.stateString.append(stringBuffer);
                    // add to list
                    if (tmpLayerSetting != null) {
                        tmpLayerSetting.filterSettings.add(tmpFilterSetting);
                    }
                }
            } else if (tagName.equals("uri")) {
                if (tmpLayerSetting != null) {
                    tmpLayerSetting.directURI += stringBuffer.toString();
                }
            } else if (tagName.equals("downloaduri")) {
                if (tmpLayerSetting != null) {
                    tmpLayerSetting.downloadURI += stringBuffer.toString();
                }
            } else if (tagName.equals("layer")) {
                fullSetting.layerSettings.add(tmpLayerSetting);
            }
            stringBuffer = new StringBuffer();
        }

        /**
         * Add a new Layer and initialize it according to the given LayerSetting
         * object, including filters
         *
         * @param layerSetting
         *            - LayerSetting describing the new layer to be set-up
         * @see LayerState
         */
        private void setupLayer(LayerState layerSetting, Interval<Date> range) {
            View newView = null;
            try {
                URI directURI = new URI(layerSetting.directURI);

                // first setup the cached API response, if available
                if (layerSetting.apiResponse != null) {
                    APIResponse apiResponse = new APIResponse(layerSetting.apiResponse, true);
                    APIResponseDump.getSingletonInstance().putResponse(apiResponse);
                }

                // If scheme is jpip, check if source was API call and file
                // still exists
                if (directURI.getScheme().equalsIgnoreCase("jpip") && (layerSetting.downloadURI.contains(Settings.getSingletonInstance().getProperty("API.jp2series.path")) || layerSetting.downloadURI.contains(Settings.getSingletonInstance().getProperty("API.jp2images.path")))) {
                    Log.info(">> LayersModel.StateParser.setupLayer() > Check if API-generated file \"" + layerSetting.directURI + "\" still exists... ");

                    URL testURL = new URL(layerSetting.directURI.replaceFirst("jpip", "http").replaceFirst(":8090", "/jp2"));
                    HttpURLConnection testConnection = (HttpURLConnection) testURL.openConnection();
                    int responseCode;
                    try {
                        testConnection.connect();
                        responseCode = testConnection.getResponseCode();
                    } catch (IOException e) {
                        responseCode = 400;
                    }

                    // If file does not exist any more -> use downloadURI to
                    // reconstruct API-call
                    if (responseCode != 200) {
                        String jpipRequest = layerSetting.downloadURI + "&jpip=true&verbose=true&linked=true";

                        Log.info(">> LayersModel.StateParser.setupLayer() > \"" + layerSetting.directURI + "\" does not exist any more.");
                        Log.info(">> LayersModel.StateParser.setupLayer() > Requesting \"" + jpipRequest + "\" instead.");

                        newView = APIRequestManager.requestData(true, new URL(jpipRequest), new URI(layerSetting.downloadURI), range, true);
                    } else { // If file exists -> Open file
                        Log.info(">> LayersModel.StateParser.setupLayer() > \"" + layerSetting.directURI + "\" still exists, load it.");
                        newView = APIRequestManager.newLoad(directURI, new URI(layerSetting.downloadURI), true, range);
                    }
                } else { // If no API file -> Open file
                    Log.info(">> LayersModel.StateParser.setupLayer() > Load file \"" + layerSetting.directURI + "\"");
                    newView = APIRequestManager.newLoad(directURI, true, range);
                }
            } catch (IOException e) {
                Message.err("An error occured while opening the file!", e.getMessage(), false);
            } catch (URISyntaxException e) {
                // This should never happen
                e.printStackTrace();
            } finally {
                // go through all sub view chains of the layered
                // view and try to find the
                // view chain of the corresponding image info view
                // TODO Markus Langenberg Can't we change the
                // APIRequestManager.newLoad to return the topmost View, instead
                // of searching it here?
                // TODO Malte Nuhn this is soo ugly!

                // check if we could load add a new layer/view
                if (newView != null) {
                    for (int i = 0; i < layeredView.getNumLayers(); i++) {
                        View subView = layeredView.getLayer(i);

                        // if view has been found
                        if (subView != null && newView.equals(subView.getAdapter(ImageInfoView.class))) {
                            newView = subView;
                            break;
                        }
                    }

                } else {
                    // this case is executed, if an error occured while adding
                    // the layer
                }
            }
        }

        /**
         * Finally setup the viewchain, filters, ... according to the internal
         * FullSetting representation
         *
         * @see FullState
         */
        public void setupLayers() {
            // First clear all Layers
            Log.info(">> LayersModel.StateParser.setupLayers() > Removing previously existing layers");
            int removedLayers = 0;
            while (LayersModel.getSingletonInstance().getNumLayers() > 0 || removedLayers > 1000) {
                LayersModel.getSingletonInstance().removeLayer(0);
                removedLayers++;
            }

            // Sort the list of layers by id
            Collections.sort(fullSetting.layerSettings);
            // reverse the list, since the layers get stacked up
            Collections.reverse(fullSetting.layerSettings);

            boolean regionIsInitialized = false;
            for (LayerState currentLayerSetting : fullSetting.layerSettings) {
                setupLayer(currentLayerSetting, null);

                // setup the region as soon as the first layer has been added
                if (!regionIsInitialized) {
                    setupRegion();
                    regionIsInitialized = false;
                }
            }
        }

        /**
         * Setup the RegionView
         */
        private void setupRegion() {
            if (fullSetting.regionViewState != null) {
                Log.info(">> LayersModel.StateParser.setupLayers() > Setting up RegionView");
                RegionViewState regionViewState = fullSetting.regionViewState;
                RegionView regionView = layeredView.getAdapter(RegionView.class);
                Region region = StaticRegion.createAdaptedRegion(regionViewState.regionX, regionViewState.regionY, regionViewState.regionWidth, regionViewState.regionHeight);
                regionView.setRegion(region, new ChangeEvent());
            } else {
                Log.info(">> LayersModel.StateParser.setupLayers() > Skipping RegionView setup.");
            }
        }

        // private classes

        /**
         * Class representing the full JHV state
         */
        private class FullState {
            /**
             * Vector of LayerStates contained in the state file
             */
            Vector<LayerState> layerSettings = new Vector<LayerState>();

            /**
             * Representation of the RegionView's region
             */
            RegionViewState regionViewState = new RegionViewState();

        }

        /**
         * Class representing the RegionView's region
         */
        private class RegionViewState {
            /**
             * Variables describing the RegionView's region
             */
            double regionX, regionY, regionWidth, regionHeight = 0;
        }

        /**
         * Class representing the state of a filter
         */
        private class FilterState {

            /**
             * Identifier of the Filter (e.g. class name)
             */
            String name = "";

            /**
             * String representing the filter's state
             */
            StringBuffer stateString = new StringBuffer("");

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "FilterSetting{ name: " + name + " state: " + stateString + "}";
            }
        }

        /**
         * Class representing the state of a layer
         */
        private class LayerState implements Comparable<LayerState> {

            /**
             * The layer's id
             */
            int id = -1;

            /**
             * Direct URI
             */
            String directURI = "";

            /**
             * Downloadable URI
             */
            String downloadURI = "";

            /**
             * API response XML
             */
            String apiResponse = null;

            /**
             * Store settings for all of this layer's filters
             */
            Vector<FilterState> filterSettings = new Vector<FilterState>();

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "LayerSetting{ id: " + id + ", URI: " + directURI + ", downloadURI: " + downloadURI + " FilterSettings: " + filterSettings + "}";
            }

            /**
             * Sort Layers by their ids.
             * <p>
             * {@inheritDoc}
             */
            @Override
            public int compareTo(LayerState other) {
                if (other != null) {
                    LayerState otherLayerSetting = other;
                    return new Integer(id).compareTo(otherLayerSetting.id);
                } else {
                    return 0;
                }
            }
        }
    }

}
