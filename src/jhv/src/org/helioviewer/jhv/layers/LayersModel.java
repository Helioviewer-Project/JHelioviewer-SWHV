package org.helioviewer.jhv.layers;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.AbstractList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.swing.JOptionPane;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.ViewListenerDistributor;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.gui.dialogs.MetaDataDialog;
import org.helioviewer.jhv.io.APIRequestManager;
import org.helioviewer.jhv.io.FileDownloader;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.ChangedReason;
import org.helioviewer.viewmodel.changeevent.LayerChangedReason;
import org.helioviewer.viewmodel.changeevent.NonConstantMetaDataChangedReason;
import org.helioviewer.viewmodel.changeevent.RegionChangedReason;
import org.helioviewer.viewmodel.changeevent.SubImageDataChangedReason;
import org.helioviewer.viewmodel.changeevent.TimestampChangedReason;
import org.helioviewer.viewmodel.changeevent.ViewChainChangedReason;
import org.helioviewer.viewmodel.changeevent.ViewportChangedReason;
import org.helioviewer.viewmodel.changeevent.LayerChangedReason.LayerChangeType;
import org.helioviewer.viewmodel.io.APIResponse;
import org.helioviewer.viewmodel.io.APIResponseDump;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.metadata.ObserverMetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.FilterView;
import org.helioviewer.viewmodel.view.ImageInfoView;
import org.helioviewer.viewmodel.view.LayeredView;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.MovieView;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.SynchronizeView;
import org.helioviewer.viewmodel.view.TimedMovieView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JP2Image;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;
import org.helioviewer.viewmodel.view.jp2view.kakadu.JHV_KduException;
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
public class LayersModel implements ViewListener {

    private int activeLayer = -1;

    /** The sole instance of this class. */
    private static final LayersModel layersModel = new LayersModel();
    private ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

    private AbstractList<LayersListener> layerListeners = new LinkedList<LayersListener>();

    // store the last updated timestamp
    private Date lastTimestamp;

    /**
     * Method returns the sole instance of this class.
     * 
     * @return the only instance of this class.
     * */
    public static LayersModel getSingletonInstance() {
        return layersModel;
    }

    public LayersModel() {
        ViewListenerDistributor.getSingletonInstance().addViewListener(this);
    }

    /**
     * Get the layeredView object. Returns null if the ImageViewerGui, the
     * mainView or the layeredView are not yet initialized.
     * 
     * @return reference to the LayeredView object, null if an error occurs
     */
    public LayeredView getLayeredView() {
        ImageViewerGui imageViewer = ImageViewerGui.getSingletonInstance();
        if (imageViewer == null)
            return null;
        ComponentView mainView = imageViewer.getMainView();
        if (mainView == null)
            return null;
        LayeredView layeredView = mainView.getAdapter(LayeredView.class);
        return layeredView;
    }

    /**
     * Return the view associated with the active Layer
     * 
     * @return View associated with the active Layer
     */
    public View getActiveView() {
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
    public View getLayer(int idx) {
        idx = invertIndex(idx);
        LayeredView lv = getLayeredView();

        if (lv != null && idx >= 0 && idx < getNumLayers()) {
            return lv.getLayer(idx);
        }

        return null;

    }

    /**
     * Set the activeLayer to the Layer that can be associated to the given
     * view, do nothing if the view cannot be associated with any layer
     * 
     * @param view
     */
    public void setActiveLayer(View view) {
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

        this.fireActiveLayerChanged(idx);
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
        View view = this.getLayer(idx);
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
    public String getCurrentFrameTimestampString(View view) {
        if (view == null) {
            return null;
        }

        MetaData md = view.getAdapter(MetaDataView.class).getMetaData();

        if (md instanceof ObserverMetaData) {
            ImmutableDateTime dt = getCurrentFrameTimestamp(view);
            return dt.getCachedDate();
        } else {
            return "N/A";
        }
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
        View view = this.getLayer(idx);
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
    public ImmutableDateTime getCurrentFrameTimestamp(View view) {
        if (view == null) {
            return null;
        }

        MetaData md = view.getAdapter(MetaDataView.class).getMetaData();

        if (md instanceof ObserverMetaData) {

            ObserverMetaData obsMetaData = (ObserverMetaData) md;
            return obsMetaData.getDateTime();

        } else {
            return null;
        }
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
    public ImmutableDateTime getStartDate(View view) {

        ImmutableDateTime result = null;
        if (view == null) {
            return result;
        }

        TimedMovieView tmv = view.getAdapter(TimedMovieView.class);

        if (tmv != null) {
            result = tmv.getFrameDateTime(0);
        } else {
            // else try to acces a timestamp by assuming it is a plain image
            result = getImageTimestamp(view);
        }

        return result;
    }

    /**
     * Retrieve the timestamp from an imageView which contains metaData
     * 
     * @param view
     * @return timestamp associated with the image data currently presented
     */
    private ImmutableDateTime getImageTimestamp(View view) {
        ImmutableDateTime result = null;

        if (view == null) {
            return null;
        }

        MetaDataView mdv = view.getAdapter(MetaDataView.class);

        if (mdv != null) {

            MetaData md = mdv.getMetaData();

            if (md instanceof ObserverMetaData) {

                ObserverMetaData omd = (ObserverMetaData) md;
                result = omd.getDateTime();

            }

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
        View view = this.getLayer(idx);
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

            if (start == null)
                continue;

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
    public ImmutableDateTime getEndDate(View view) {

        ImmutableDateTime result = null;

        if (view == null) {
            return result;
        }

        TimedMovieView tmv = view.getAdapter(TimedMovieView.class);

        if (tmv != null) {
            int lastFrame = tmv.getMaximumFrameNumber();
            // the following call will block if the meta is not yet available -
            // and will cause deadlocks if called form the wrong thread
            result = tmv.getFrameDateTime(lastFrame);
        } else {
            // else try to acces a timestamp by assuming it is a plain image
            result = getImageTimestamp(view);
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

            if (end == null)
                continue;

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
        View view = this.getLayer(idx);
        return this.getEndDate(view);
    }

    /**
     * Find the index of the layer that can be associated with the given view
     * 
     * @param view
     *            - View that can be associated with the layer in question
     * @return index of the layer that can be associated with the given view
     */
    public int findView(View view) {
        LayeredView lv = getLayeredView();
        View theView = ViewHelper.findLastViewBeforeLayeredView(view);
        int idx = lv.getLayerLevel(theView);
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
        // invert indices
        if (idx >= 0 && this.getNumLayers() > 0) {
            idx = this.getNumLayers() - 1 - idx;
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
        // invert indices, based on the indices before one layer was removed
        if (idx >= 0 && this.getNumLayers() >= 0) {
            idx = this.getNumLayers() - idx;
        }

        return idx;
    }

    /**
     * Return the number of layers currently available
     * 
     * @return number of layers
     */
    public int getNumLayers() {
        LayeredView lv = getLayeredView();

        if (lv != null) {
            return lv.getNumLayers();
        } else {
            return 0;
        }
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
    public void setVisibleLink(View view, boolean visible) {
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
        View view = this.getLayer(idx);
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
    public void setVisible(View view, boolean visible) {
        LayeredView lv = getLayeredView();
        if (lv != null) {
            if (lv.isVisible(view) != visible) {
                lv.toggleVisibility(view);
            }
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
        LayeredView lv = getLayeredView();
        if (lv != null) {
            if (lv.isVisible(getLayer(idx)) != visible) {
                lv.toggleVisibility(getLayer(idx));
            }
        }
    }

    /**
     * Get the visibility of the layer in question
     * 
     * @param view
     *            - View that can be associated with the layer in question
     * 
     * @return true if the layer is visible
     */
    public boolean isVisible(View view) {
        LayeredView lv = getLayeredView();
        if (lv != null) {
            return lv.isVisible(view);
        } else {
            return false;
        }
    }

    /**
     * Get the visibility of the layer in question
     * 
     * @param idx
     *            - index of the layer in question
     * @return true if the layer is visible
     */
    public boolean isVisible(int idx) {
        LayeredView lv = getLayeredView();
        if (lv != null) {
            return lv.isVisible(getLayer(idx));
        } else {
            return false;
        }
    }

    /**
     * Get the name of the layer in question
     * 
     * @param view
     *            - View that can be associated with the layer in question
     * @return name of the layer, the views default String representation if no
     *         name is available
     */
    public String getName(View view) {
        if (view == null) {
            return null;
        }
        ImageInfoView imageInfoView = view.getAdapter(ImageInfoView.class);
        if (imageInfoView != null) {
            return imageInfoView.getName();
        } else {
            return view.toString();
        }
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
        View view = this.getLayer(idx);
        return this.getName(view);
    }

    private void handleLayerChanges(View sender, ChangeEvent aEvent) {
        LayerChangedReason layerReason = aEvent.getLastChangedReasonByType(LayerChangedReason.class);

        // if layers were changed, perform same operations on GUI
        if (layerReason != null) {
            // If layer was deleted, delete corresponding panel
            if ((!layerReason.getProcessed()) && layerReason.getLayerChangeType() == LayerChangedReason.LayerChangeType.LAYER_ADDED) {
                layerReason.setProcessed(true);
                View view = layerReason.getSubView();
                int newIndex = findView(view);
                if (newIndex != -1) {
                    this.setActiveLayer(newIndex);
                    this.fireLayerAdded(newIndex);
                }
            } else if ((!layerReason.getProcessed()) && layerReason.getLayerChangeType() == LayerChangedReason.LayerChangeType.LAYER_REMOVED) {
                layerReason.setProcessed(true);
                int oldIndex = this.invertIndexDeleted(layerReason.getLayerIndex());
                this.fireLayerRemoved(layerReason.getView(), oldIndex);
                int newIndex = determineNewActiveLayer(oldIndex);
                this.setActiveLayer(newIndex);

            } else if ((!layerReason.getProcessed()) && layerReason.getLayerChangeType() == LayerChangedReason.LayerChangeType.LAYER_VISIBILITY) {
                layerReason.setProcessed(true);
                View view = layerReason.getSubView();
                int idx = findView(view);
                if (idx != -1) {
                    this.fireLayerChanged(idx);
                }
            } else if ((!layerReason.getProcessed()) && layerReason.getLayerChangeType() == LayerChangedReason.LayerChangeType.LAYER_DOWNLOADED) {
                layerReason.setProcessed(true);
                View view = layerReason.getSubView();
                int idx = findView(view);
                if (idx != -1) {
                    this.fireLayerDownloaded(idx);
                }
            }
        }
    }

    private void handleViewportPositionChanges(View sender, ChangeEvent aEvent) {
        ChangedReason reason = aEvent.getLastChangedReasonByType(NonConstantMetaDataChangedReason.class);
        ChangedReason reason2 = aEvent.getLastChangedReasonByType(RegionChangedReason.class);
        ChangedReason reason3 = aEvent.getLastChangedReasonByType(ViewportChangedReason.class);

        if (reason != null || reason2 != null || reason3 != null) {
            this.fireViewportGeometryChanged();
        }

    }

    private void handleTimestampChanges(View sender, ChangeEvent aEvent) {
        List<TimestampChangedReason> timestampReasons = aEvent.getAllChangedReasonsByType(TimestampChangedReason.class);

        // if meta data has changed, update label
        for (TimestampChangedReason timestampReason : timestampReasons) {
            if (timestampReason.getView() != null) {
                View timestampView = timestampReason.getView();

                int idx = findView(timestampView);
                if (isValidIndex(idx)) {
                    this.fireTimestampChanged(idx);

                    // store last timestamp displayed
                    ImmutableDateTime currentFrameTimestamp = getCurrentFrameTimestamp(idx);
                    if (currentFrameTimestamp != null) {
                        lastTimestamp = currentFrameTimestamp.getTime();
                    } else {
                        currentFrameTimestamp = null;
                    }
                }

            }
        }
    }

    private void handleSubImageDataChanges(View sender, ChangeEvent aEvent) {
        if (aEvent.reasonOccurred(SubImageDataChangedReason.class)) {
            this.fireSubImageDataChanged();
        }
    }

    private void handleViewChainChanges(View sender, ChangeEvent aEvent) {
        if (aEvent.getLastChangedReasonByType(ViewChainChangedReason.class) != null) {
            this.fireAllLayersChanged();
        }
    }

    /**
     * View changed handler.
     * 
     * Internally forwards (an abstraction) of the events to the LayersListener
     * 
     */
    public void viewChanged(View sender, ChangeEvent aEvent) {

        handleSubImageDataChanges(sender, aEvent);
        handleTimestampChanges(sender, aEvent);
        handleViewportPositionChanges(sender, aEvent);
        handleLayerChanges(sender, aEvent);
        handleViewChainChanges(sender, aEvent);
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
        View view = getLayeredView().getLayer(idx);
        downloadLayer(view);
    }

    /**
     * Trigger downloading the layer in question
     * 
     * @param view
     *            - View that can be associated with the layer in question
     */
    public void downloadLayer(final View view) {
        if (view == null) {
            return;
        }
        final ImageInfoView infoView = view.getAdapter(ImageInfoView.class);

        Thread downloadThread = new Thread(new Runnable() {
            public void run() {
                downloadFromJPIP(infoView);
                LayersModel.getSingletonInstance().viewChanged(infoView, new ChangeEvent(new LayerChangedReason(infoView, LayerChangeType.LAYER_DOWNLOADED, view)));
            }
        }, "DownloadFromJPIPThread");
        downloadThread.start();
    }

    /**
     * Downloads the complete image from the JPIP server.
     * 
     * Changes the source of the ImageInfoView afterwards, since a local file is
     * always faster.
     */
    private void downloadFromJPIP(ImageInfoView view) {
        if (view == null) {
            return;
        }

        FileDownloader fileDownloader = new FileDownloader();
        URI source = view.getAdapter(ImageInfoView.class).getDownloadURI();

        // the http server to download the file from is unknown
        if (view.getAdapter(ImageInfoView.class).getDownloadURI().equals(view.getAdapter(ImageInfoView.class).getUri()) && !view.getAdapter(ImageInfoView.class).getDownloadURI().toString().contains("delphi.nascom.nasa.gov")) {

            String inputValue = JOptionPane.showInputDialog("To download this file, please specify a concurrent HTTP server address to the JPIP server: ", view.getAdapter(ImageInfoView.class).getUri());
            if (inputValue != null) {
                try {
                    source = new URI(inputValue);
                } catch (URISyntaxException e) {
                }
            }
        }

        File downloadDestination = fileDownloader.getDefaultDownloadLocation(view.getAdapter(ImageInfoView.class).getUri());
        JHVJP2View mainView = view.getAdapter(JHVJP2View.class);
        JHVJP2View overviewView = (JHVJP2View) ImageViewerGui.getSingletonInstance().getOverviewView().getAdapter(SynchronizeView.class).getCorrespondingView(mainView);
        try {

            if (!fileDownloader.get(source, downloadDestination, "Downloading " + view.getAdapter(ImageInfoView.class).getName())) {
                return;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try {
            ImageViewerGui.getSingletonInstance().getMainImagePanel().setLoading(true);
            JP2Image localImage = new JP2Image(downloadDestination.toURI());
            mainView.setJP2Image(localImage);
            overviewView.setJP2Image(localImage);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JHV_KduException e) {
            e.printStackTrace();
        } finally {
            ImageViewerGui.getSingletonInstance().getMainImagePanel().setLoading(false);
        }

        /*
         * if(view.getAdapter(ImageInfoView.class).getUri().getScheme().
         * equalsIgnoreCase("file")) { this.fireLayerChanged(getNumLayers()); }
         */
    }

    /**
     * Trigger showing a dialog displaying the meta data of the layer in
     * question.
     * 
     * @param idx
     *            - index of the layer in question
     */
    public void showMetaInfo(int idx) {
        View view = getLayer(idx);
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
    public void showMetaInfo(View view) {
        if (view == null) {
            return;
        }
        MetaDataDialog dialog = new MetaDataDialog();
        dialog.setMetaData(view.getAdapter(MetaDataView.class));
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
        LayeredView lv = getLayeredView();
        lv.removeLayer(idx);
        Log.debug(">> LayersModel.removeLayer()");
    }

    /**
     * Remove the layer in question
     * 
     * @param view
     *            - View that can be associated with the layer in question
     */
    public void removeLayer(View view) {
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
    public void setLink(View view, boolean link) {
        if (view == null) {
            return;
        }

        if (!this.isTimed(view))
            return;

        MovieView view2 = view.getAdapter(MovieView.class);

        if (view2 != null) {
            MoviePanel moviePanel = MoviePanel.getMoviePanel(view2);
            if (moviePanel != null) {
                moviePanel.setMovieLink(link);
            }
        }

        this.fireAllLayersChanged();

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
        View view = this.getLayer(idx);
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
        View view = this.getLayer(idx);
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
    public void setPlaying(View view, boolean play) {
        if (view == null) {
            return;
        }

        TimedMovieView timedMovieView = view.getAdapter(TimedMovieView.class);

        if (timedMovieView != null) {
            if (play) {
                timedMovieView.playMovie();
            } else {
                timedMovieView.pauseMovie();
            }
        }
    }

    /**
     * Check whether the layer in question is a movie
     * 
     * @param idx
     *            - index of the layer in question
     * @return true if the layer in question is a movie
     */
    public boolean isMovie(int idx) {
        Log.debug(">> LayersModel.isMovie(" + idx + ")");
        View view = this.getLayer(idx);
        return isMovie(view);
    }

    /**
     * Check whether the layer in question is a movie
     * 
     * @param view
     *            - View that can be associated with the layer in question
     * @return true if the layer in question is a movie
     */
    public boolean isMovie(View view) {
        if (view == null) {
            return false;
        }
        MovieView view2 = view.getAdapter(MovieView.class);
        return (view2 != null);
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
    public boolean isTimed(View view) {
        if (view == null) {
            return false;
        }
        MetaDataView metaDataView = view.getAdapter(MetaDataView.class);

        if (metaDataView != null) {
            MetaData md = view.getAdapter(MetaDataView.class).getMetaData();
            return (md instanceof ObserverMetaData);
        } else {
            return false;
        }
    }

    /**
     * Move the layer in question up
     * 
     * @param view
     *            - View that can be associated with the layer in question
     */
    public void moveLayerUp(View view) {
        if (view == null) {
            return;
        }
        // Operates on the (inverted) LayeredView indices
        LayeredView lv = this.getLayeredView();

        int level = lv.getLayerLevel(view);

        if (level < lv.getNumLayers() - 1) {
            level++;
        }

        lv.moveView(view, level);
        this.setActiveLayer(invertIndex(level));

    }

    /**
     * Move the layer in question down
     * 
     * @param view
     *            - View that can be associated with the layer in question
     */
    public void moveLayerDown(View view) {
        if (view == null) {
            return;
        }
        // Operates on the (inverted) LayeredView indices
        LayeredView lv = this.getLayeredView();

        int level = lv.getLayerLevel(view);

        if (level > 0) {
            level--;
        }

        lv.moveView(view, level);
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
        View view = getLayer(idx);
        return isPlaying(view);
    }

    /**
     * Check whether the layer in question is currently playing
     * 
     * @param view
     *            - View that can be associated with the layer in question
     * @return true if the layer in question is currently playing
     */
    public boolean isPlaying(View view) {
        if (view == null) {
            return false;
        }
        if (isMovie(view)) {
            MovieView movieView = view.getAdapter(MovieView.class);
            return movieView.isMoviePlaying();
        } else {
            return false;
        }
    }

    /**
     * Return the current framerate for the layer in question
     * 
     * @param idx
     *            - index of the layer in question
     * @return the current framerate or 0.0 if the movie is not playing, or if
     *         an error occurs
     */
    public double getFPS(int idx) {
        View view = getLayer(idx);
        return getFPS(view);
    }

    /**
     * Return the current framerate for the layer in question
     * 
     * @param view
     *            - View that can be associated with the layer in question
     * @return the current framerate or 0.0 if the movie is not playing, or if
     *         an error occurs
     */
    public double getFPS(View view) {
        double result = 0.0;

        if (view == null) {
            return result;
        }

        if (isMovie(view)) {
            MovieView movieView = view.getAdapter(MovieView.class);
            if (isPlaying(view)) {
                result = Math.round(movieView.getActualFramerate() * 100) / 100;
            }
        }

        return result;

    }

    /**
     * Check whether the layer in question is a Master in the list of linked
     * movies
     * 
     * @param idx
     *            - index of the layer in question
     * @return true if the layer in question is a master
     */
    public boolean isMaster(int idx) {
        View view = getLayer(idx);
        return isMaster(view);
    }

    /**
     * Check whether the layer in question is a Master in the list of linked
     * movies
     * 
     * @param view
     *            - View that can be associated with the layer in question
     * @return true if the layer in question is a master
     */
    public boolean isMaster(View view) {
        if (view == null) {
            return false;
        }

        TimedMovieView timedMovieView = view.getAdapter(TimedMovieView.class);
        return LinkedMovieManager.getActiveInstance().isMaster(timedMovieView);
    }

    /**
     * Check whether the layer in question is a Remote View
     * 
     * @param view
     *            - View that can be associated with the layer in question
     * @return true if the layer in question is a remote view
     */
    public boolean isRemote(View view) {
        if (view == null) {
            return false;
        }

        JHVJP2View jp2View = view.getAdapter(JHVJP2View.class);
        if (jp2View != null) {
            return jp2View.isRemote();
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
        View view = getLayer(idx);
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
        View view = getLayer(idx);
        return isConnectedToJPIP(view);
    }

    /**
     * Check whether the layer in question is connected to a JPIP server
     * 
     * @param view
     *            - View that can be associated with the layer in question
     * @return true if the layer is connected to a JPIP server
     */
    public boolean isConnectedToJPIP(View view) {
        if (view == null) {
            return false;
        }

        JHVJP2View jp2View = view.getAdapter(JHVJP2View.class);
        if (jp2View != null) {
            return jp2View.isConnectedToJPIP();
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
        View view = this.getLayer(idx);
        return getDescriptor(view);
    }

    /**
     * Return a representation of the layer in question
     * 
     * @param view
     *            - View that can be associated with the layer in question
     * @return LayerDescriptor of the current state of the layer in question
     */
    public LayerDescriptor getDescriptor(View view) {
        ImageInfoView imageInfoView = view.getAdapter(ImageInfoView.class);
        String typeString;
        String intervalString;

        if (imageInfoView != null) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm");
            Interval<Date> interval = imageInfoView.getDateRange();
            if (interval != null) {
                typeString = "JPEG2000-movie";
                String beginDate = format.format(interval.getStart());
                String endDate = format.format(interval.getEnd());
                intervalString = beginDate + "-" + endDate;
            } else {
                intervalString = layersModel.getCurrentFrameTimestampString(view);
                typeString = "Single image";
            }
        } else {
            intervalString = layersModel.getCurrentFrameTimestampString(view);
            typeString = "JPEG200-movie";
        }
        LayerDescriptor ld = new LayerDescriptor(intervalString, typeString);

        ld.isMovie = layersModel.isMovie(view);
        ld.isMaster = layersModel.isMaster(view);
        ld.isVisible = layersModel.isVisible(view);
        ld.isTimed = layersModel.isTimed(view);
        ld.title = layersModel.getName(view);
        ld.timestamp = layersModel.getCurrentFrameTimestampString(view);

        return ld;
    }

    private void fireLayerDownloaded(final int index) {
        Thread thread = new Thread(new Runnable() {

            public void run() {
                rwl.readLock().lock();
                try {
                    for (LayersListener ll : layerListeners) {
                        ll.layerDownloaded(index);
                    }
                } finally {
                    rwl.readLock().unlock();
                }
            }
        }, "LayersModelThread (layer removed)");

        thread.start();
    }

    /**
     * Notify all LayersListeners
     */
    private void fireLayerRemoved(final View oldView, final int oldIndex) {
        Thread thread = new Thread(new Runnable() {

            public void run() {
                rwl.readLock().lock();
                try {
                    for (LayersListener ll : layerListeners) {
                        ll.layerRemoved(oldView, oldIndex);
                    }
                } finally {
                    rwl.readLock().unlock();
                }
            }
        }, "LayersModelThread (layer removed)");

        thread.start();

    }

    /**
     * Notify all LayersListeners
     */
    private void fireLayerAdded(final int newIndex) {

        Thread thread = new Thread(new Runnable() {

            public void run() {
                rwl.readLock().lock();
                try {
                    for (LayersListener ll : layerListeners) {
                        ll.layerAdded(newIndex);
                    }
                } finally {
                    rwl.readLock().unlock();
                }
            }
        }, "LayersModelThread (layer added)");

        thread.start();
    }

    /**
     * Notify all LayersListeners
     */
    private void fireLayerChanged(final int index) {

        Thread thread = new Thread(new Runnable() {

            public void run() {
                rwl.readLock().lock();
                try {
                    for (LayersListener ll : layerListeners) {
                        ll.layerChanged(index);
                    }
                } finally {
                    rwl.readLock().unlock();
                }
            }
        }, "LayersModelThread (single layer changed)");

        thread.start();

    }

    /**
     * Notify all LayersListeners
     */
    private void fireAllLayersChanged() {

        Thread thread = new Thread(new Runnable() {

            public void run() {
                rwl.readLock().lock();
                try {
                    for (LayersListener ll : layerListeners) {
                        for (int index = 0; index < getNumLayers(); index++) {
                            ll.layerChanged(index);
                        }
                    }
                } finally {
                    rwl.readLock().unlock();
                }
            }
        }, "LayersModelThread (all layers changed)");

        thread.start();

    }

    /**
     * Notify all LayersListeners
     */
    private void fireActiveLayerChanged(final int index) {
        Thread thread = new Thread(new Runnable() {

            public void run() {
                rwl.readLock().lock();
                try {
                    for (LayersListener ll : layerListeners) {
                        ll.activeLayerChanged(index);
                    }
                } finally {
                    rwl.readLock().unlock();
                }
            }
        }, "LayersModelThread (active layer changed)");

        thread.start();
    }

    /**
     * Notify all LayersListeners
     */
    private void fireViewportGeometryChanged() {
        Thread thread = new Thread(new Runnable() {

            public void run() {
                rwl.readLock().lock();
                try {
                    for (LayersListener ll : layerListeners) {
                        ll.viewportGeometryChanged();
                    }
                } finally {
                    rwl.readLock().unlock();
                }
            }
        }, "LayersModelThread (viewport geometry changed)");

        thread.start();

    }

    /**
     * Notify all LayersListeners
     */
    private void fireTimestampChanged(final int idx) {
        Thread thread = new Thread(new Runnable() {

            public void run() {
                rwl.readLock().lock();
                try {
                    for (LayersListener ll : layerListeners) {
                        ll.timestampChanged(idx);
                    }
                } finally {
                    rwl.readLock().unlock();
                }
            }
        }, "LayersModelThread (timestamp changed)");

        thread.start();

    }

    /**
     * Notify all LayersListeners
     */
    private void fireSubImageDataChanged() {
        Thread thread = new Thread(new Runnable() {

            public void run() {
                rwl.readLock().lock();
                try {
                    for (LayersListener ll : layerListeners) {
                        ll.subImageDataChanged();
                    }
                } finally {
                    rwl.readLock().unlock();
                }
            }
        }, "LayersModelThread (subimage data changed)");

        thread.start();
    }

    /**
     * Notify all LayersListeners
     */
    public void addLayersListener(LayersListener layerListener) {
        rwl.writeLock().lock();
        try {
            layerListeners.add(layerListener);
        } finally {
            rwl.writeLock().unlock();
        }
    }

    /**
     * Remove LayersListener
     * 
     * * @author Carlos Martin
     */
    public void removeLayersListener(LayersListener layerListener) {
        rwl.writeLock().lock();
        try {
            layerListeners.remove(layerListener);
        } finally {
            rwl.writeLock().unlock();
        }
    }

    /**
     * Get last Frame
     * 
     * @return
     */
    public Date getLastUpdatedTimestamp() {
        if (lastTimestamp == null) {
            Date lastDate = this.getLastDate();
            if (lastDate != null) {
                lastTimestamp = this.getLastDate();
                return lastTimestamp;
            }
            return null;
        } else {
            return lastTimestamp;
        }
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
        rwl.readLock().lock();
        try {
            StringBuffer xml = new StringBuffer();

            int layers = LayersModel.getSingletonInstance().getNumLayers();

            if (layers == 0) {
                return "";
            }

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
                    FilterView currentFilterView = currentView.getAdapter(FilterView.class);

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

                    xml.append(tab).append("<filters>\n");

                    // add tab
                    tab = tab + "\t";
                    while (currentFilterView != null) {
                        String filterName = currentFilterView.getFilter().getClass().getName();
                        String filterState = currentFilterView.getFilter().getState();
                        xml.append(tab).append("<filter name=\"").append(filterName).append("\">").append(filterState).append("</filter>\n");
                        currentFilterView = currentFilterView.getView().getAdapter(FilterView.class);
                    }

                    // remove last tab
                    tab = tab.substring(0, tab.length() - 1);
                    xml.append(tab).append("</filters>\n");

                    // remove last tab
                    tab = tab.substring(0, tab.length() - 1);
                    xml.append(tab).append("</layer>\n");
                }
            }

            // remove last tab
            tab = tab.substring(0, tab.length() - 1);

            xml.append(tab).append("</layers>");

            return xml.toString();

        } finally {
            rwl.readLock().unlock();
        }
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
        private FullState fullSetting = new FullState();

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
        public void characters(char ch[], int start, int length) throws SAXException {
            stringBuffer.append(new String(ch, start, length));
        }

        /**
         * {@inheritDoc}
         */
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
         * Find and Setup the Filter with the given name that is associated with
         * the given view and set it's state according to the given StateString
         * 
         * @param view
         *            - View to which the filter in question can be associated
         *            with
         * @param filterName
         *            - Class name of the filter in question
         * @param filterState
         *            - State string to be passed to the filter in question
         */
        private void setupFilters(View view, Vector<FilterState> filterStates) {
            FilterView currentFilterView = view.getAdapter(FilterView.class);

            // loop over all FilterView objects reachable from the given view
            while (currentFilterView != null) {
                String curFilterName = currentFilterView.getFilter().getClass().getName();

                // Loop over all available filter states
                for (FilterState curFilterState : filterStates) {

                    // if we found a suitable filter state, apply it
                    if (curFilterState.name.equals(curFilterName)) {
                        currentFilterView.getFilter().setState(curFilterState.stateString.toString());
                    }
                }

                currentFilterView = currentFilterView.getView().getAdapter(FilterView.class);
            }
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

                    LayeredView layeredView = LayersModel.getSingletonInstance().getLayeredView();
                    for (int i = 0; i < layeredView.getNumLayers(); i++) {
                        View subView = layeredView.getLayer(i);

                        // if view has been found
                        if (subView != null && newView.equals(subView.getAdapter(ImageInfoView.class))) {
                            newView = subView;
                            break;
                        }
                    }

                    // newView should always be != null
                    if (newView != null) {
                        setupFilters(newView, layerSetting.filterSettings);
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
                RegionView regionView = LayersModel.getSingletonInstance().getLayeredView().getAdapter(RegionView.class);
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
            public String toString() {
                return "LayerSetting{ id: " + id + ", URI: " + directURI + ", downloadURI: " + downloadURI + " FilterSettings: " + filterSettings + "}";
            }

            /**
             * Sort Layers by their ids.
             * <p>
             * {@inheritDoc}
             */
            public int compareTo(LayerState other) {
                if (other != null) {
                    LayerState otherLayerSetting = (LayerState) other;
                    return new Integer(id).compareTo(otherLayerSetting.id);
                } else {
                    return 0;
                }
            }
        }
    }
}
