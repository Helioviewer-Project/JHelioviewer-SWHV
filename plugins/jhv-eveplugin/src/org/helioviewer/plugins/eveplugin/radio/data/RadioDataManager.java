package org.helioviewer.plugins.eveplugin.radio.data;

import java.awt.Rectangle;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.plugins.eveplugin.EVEState;
import org.helioviewer.plugins.eveplugin.radio.model.ResolutionSetting;
import org.helioviewer.plugins.eveplugin.radio.model.YValueModel;
import org.helioviewer.plugins.eveplugin.radio.model.YValueModelManager;
import org.helioviewer.plugins.eveplugin.radio.model.ZoomManager;
import org.helioviewer.plugins.eveplugin.settings.EVESettings;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.region.RegionAdapter;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View.ReaderMode;
import org.helioviewer.viewmodel.view.jp2view.JP2Image;
import org.helioviewer.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.viewmodel.viewport.StaticViewport;
import org.helioviewer.viewmodel.viewport.ViewportAdapter;

/**
 * The radio data manager manages all the downloaded data for radio
 * spectrograms.
 * 
 * It receives all of its input from the radio downloader as listener of the
 * radio downloader.
 * 
 * @author Bram.Bourgoignie@oma.be
 * 
 */
public class RadioDataManager implements RadioDownloaderListener {

    /** The singleton instance of the class. */
    private static RadioDataManager instance;

    /** A list collecting all the radio data manager listeners. */
    private List<RadioDataManagerListener> listeners;

    /** A map keeping the download request data. */
    private Map<Long, DownloadRequestData> downloadRequestData;

    /** Instance of the radio downloader */
    private RadioDownloader downloader;

    /** A buffer holding all the requests for data. */
    private RequestForDataBuffer requestBuffer;

    /** The instance of the line data selector model */
    private LineDataSelectorModel lineDataSelectorModel;

    /** Instance of the radio image cache */
    private RadioImageCache cache;

    /** Instance of the zoom manager */
    private ZoomManager zoomManager;

    /** Id for identifying the requests for data. */
    private long id = 0;

    /** Instance of eve state */
    private EVEState eveState;

    /** INstance of the y-value model manager */
    private YValueModelManager yValueModelManager;

    /**
     * private constructor used when the instance is requested for the first
     * time.
     * 
     */
    private RadioDataManager() {
        listeners = new ArrayList<RadioDataManagerListener>();
        downloadRequestData = new HashMap<Long, DownloadRequestData>();
        downloader = RadioDownloader.getSingletonInstance();
        downloader.addRadioDownloaderListener(this);
        lineDataSelectorModel = LineDataSelectorModel.getSingletonInstance();
        cache = RadioImageCache.getInstance();
        requestBuffer = new RequestForDataBuffer();
        zoomManager = ZoomManager.getSingletonInstance();
        eveState = EVEState.getSingletonInstance();
        yValueModelManager = YValueModelManager.getInstance();
    }

    /**
     * Access to the singleton instance of the radio data manager
     * 
     * @return The instance of the radio data manager
     */
    public static RadioDataManager getSingletonInstance() {
        if (instance == null) {
            instance = new RadioDataManager();
        }
        return instance;
    }

    /**
     * Adds a radio data manager listener.
     * 
     * @param l
     *            The listener to add
     */
    public void addRadioManagerListener(RadioDataManagerListener l) {
        listeners.add(l);
    }

    /**
     * Removes a radio data manager listener.
     * 
     * @param l
     *            The listener to remove
     */
    public void removeRadioManagerListener(RadioDataManagerListener l) {
        listeners.remove(l);
    }

    /**
     * Inform the RadioDataManager about the new data that was received. The
     * RadioDataManagerListeners will be informed about the new received data.
     * 
     * 
     * @param data
     *            The data received
     * @param imageID
     *            The ID of the image for which data was received
     * @param downloadID
     *            The ID of the download batch the image is part of
     * @param dataSize
     *            The height and width of the data
     */
    public synchronized void dataForIDReceived(byte[] data, Long imageID, Long downloadID, Rectangle dataSize) {
        fireDataforIDReceived(data, imageID, downloadID, dataSize);
    }

    /**
     * Removes the given download request data. The data is removed from the
     * line selector model. The images are removed from the cache.
     * 
     * The listeners are informed about the removal of the download request
     * data.
     * 
     * @param drd
     *            The download request data that should be removed
     */
    public void removeDownloadRequestData(DownloadRequestData drd) {
        this.downloadRequestData.remove(drd.getDownloadID());
        lineDataSelectorModel.removeLineData(drd);
        for (Long imageID : drd.getRadioImages().keySet()) {
            cache.remove(imageID, drd.getPlotIdentifier());
        }
        fireDownloadRequestDataRemoved(drd);
    }

    /**
     * Changed the visibility of the download request data. All radio data
     * manager listeners are informed.
     * 
     * 
     * @param drd
     *            The download request data for which the visiblility is changed
     */
    public void downloadRequestDataVisibilityChanged(DownloadRequestData drd) {
        fireDownloadRequestDataVisibilityChanged(drd);
    }

    /**
     * Request the radio data manager for new data.
     * 
     * As long as the mouse is dragged no new data is requested. This to improve
     * the responsiveness of the program.
     * 
     * This function can be called asynchronously. A request buffer is used to
     * store the all the calls. As long as there are requests in the buffer
     * function continues.
     * 
     * @param xStart
     *            The start time for which new data is requested
     * @param xEnd
     *            The end time for which new data is requested
     * @param yStart
     *            The start frequency for which new data is requested
     * @param yEnd
     *            The end frequency for which new data is requested
     * @param xRatio
     *            The x-ratio (time per pixel)
     * @param yRatio
     *            The y-ratio (frequency per pixel)
     * @param iDs
     *            The download ID-s for which new data is requested
     * @param plotIdentifier
     *            The identifier of the plot for which new data is requested
     */
    public void requestForData(Date xStart, Date xEnd, double yStart, double yEnd, double xRatio, double yRatio, List<Long> iDs, String plotIdentifier) {
        Long start = System.currentTimeMillis();
        Log.trace("Request for data : " + id + " time " + start);
        if (!eveState.isMouseTimeIntervalDragging() && !eveState.isMouseValueIntervalDragging()) {
            Log.trace("mouse is not dragged");
            if (!requestBuffer.hasData()) {
                requestBuffer.addRequestConfig(new RequestConfig(xStart, xEnd, yStart, yEnd, xRatio, yRatio, iDs));
                while (requestBuffer.hasData()) {
                    RequestConfig requestConfig = requestBuffer.getData();
                    synchronized (downloadRequestData) {
                        handleRequestConfig(requestConfig, xStart, xEnd, yStart, yEnd, plotIdentifier);
                    }
                }
            } else {
                Log.trace("Add request to buffer");
                requestBuffer.addRequestConfig(new RequestConfig(xStart, xEnd, yStart, yEnd, xRatio, yRatio, iDs));
            }
        } else {
            Log.trace("Mouse is dragged");
        }
        long end = System.currentTimeMillis();
        Log.trace("Finished request for data id: " + id + " time : " + end + " in " + (end - start) + " milliseconds");
        id++;
    }

    /**
     * Informs the radio data manager about an image, identified by imageID from
     * download identified by downloadID, finished its downloading?
     * 
     * 
     * @param imageID
     *            The image id of the image that finished downloading
     * @param downloadID
     *            The download id of the batch of which the image was part
     */
    public void finishedDownloadingID(Long imageID, Long downloadID) {
        synchronized (downloadRequestData) {
            DownloadRequestData drd = this.downloadRequestData.get(downloadID);
            if (drd != null) {
                RadioImage image = drd.getRadioImages().get(imageID);
                if (image != null) {
                    image.setDownloading(false);
                }
                boolean isDownloading = false;
                for (RadioImage im : drd.getRadioImages().values()) {
                    if (im.isDownloading()) {
                        isDownloading = true;
                        break;
                    }
                }
                if (!isDownloading) {
                    drd.setDownloading(false);
                    lineDataSelectorModel.downloadFinished(drd);
                }
            }
        }
    }

    /*
     * RadioDownloadListener
     */
    @Override
    public void intervalTooBig(Date requestedStartTime, Date requestedEndTime, long ID, String identifier) {
        DownloadRequestData drd = new DownloadRequestData(ID, identifier);
        downloadRequestData.put(ID, drd);
        lineDataSelectorModel.addLineData(drd);
        fireIntervalTooBig(ID, identifier);
        fireNewDataAvailable(drd, ID);
        fireDownloadRequestAnswered(new Interval<Date>(requestedStartTime, requestedEndTime), ID, identifier);

    }

    @Override
    public void newJPXFilesDownloaded(List<DownloadedJPXData> jpxFiles, Date requestedStartTime, Date requestedEndTime, Long downloadID, String plotIdentifier) {
        Log.trace("Init the download request data in radio data manager");
        synchronized (downloadRequestData) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            DownloadRequestData drd = new DownloadRequestData(downloadID, plotIdentifier);
            drd.setDownloading(true);
            lineDataSelectorModel.addLineData(drd);
            if (!jpxFiles.isEmpty()) {
                for (DownloadedJPXData djd : jpxFiles) {
                    handleDownloadedJPXData(djd, sdf, drd, downloadID, plotIdentifier);
                }
            }
            this.downloadRequestData.put(downloadID, drd);
            defineMaxBounds(downloadID, plotIdentifier);
            fireNewDataAvailable(drd, downloadID);
            fireDownloadRequestAnswered(new Interval<Date>(requestedStartTime, requestedEndTime), downloadID, plotIdentifier);
        }
    }

    @Override
    public void newAdditionalDataDownloaded(List<DownloadedJPXData> jpxFiles, Long downloadID, String plotIdentifier, double ratioX, double ratioY) {
        synchronized (downloadRequestData) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            DownloadRequestData drd = downloadRequestData.get(downloadID);
            drd.setDownloading(true);
            lineDataSelectorModel.downloadStarted(drd);
            for (DownloadedJPXData djd : jpxFiles) {
                handleAdditionalJPXData(djd, ratioX, ratioY, downloadID, drd, sdf, plotIdentifier);
            }
            this.downloadRequestData.put(downloadID, drd);
            defineMaxBounds(downloadID, plotIdentifier);
            fireNewDataAvailable(drd, downloadID);
        }
    }

    @Override
    public void newNoData(List<Interval<Date>> noDataList, String identifier, long downloadID) {
        if (!eveState.isMouseTimeIntervalDragging() && !eveState.isMouseValueIntervalDragging()) {
            fireNoDataIntervalsReceived(noDataList, downloadID, identifier);
        }
    }

    /**
     * Define the maximum bound of the frequency interval. The y-value model is
     * updated.
     * 
     * @param downloadID
     *            The download ID for which the bounds are defined
     * @param plotIdentifier
     *            The plot identifier for which the bound are defined
     */
    private void defineMaxBounds(Long downloadID, String plotIdentifier) {
        synchronized (downloadRequestData) {
            FrequencyInterval maxFrequencyInterval = new FrequencyInterval();
            DownloadRequestData drd = this.downloadRequestData.get(downloadID);
            if (drd != null) {
                Map<Long, RadioImage> radioImages = drd.getRadioImages();
                if (!radioImages.isEmpty()) {
                    int localMinFrequency = -1;
                    int localMaxFrequency = -1;
                    boolean first = true;
                    for (RadioImage image : radioImages.values()) {
                        if (first) {
                            localMinFrequency = image.getFreqInterval().getStart();
                            localMaxFrequency = image.getFreqInterval().getEnd();
                            first = false;
                        } else {
                            if (image.getFreqInterval().getStart() < localMinFrequency) {
                                localMinFrequency = image.getFreqInterval().getStart();
                            }
                            if (image.getFreqInterval().getEnd() > localMaxFrequency) {
                                localMaxFrequency = image.getFreqInterval().getEnd();
                            }
                        }
                    }
                    maxFrequencyInterval = new FrequencyInterval(localMinFrequency, localMaxFrequency);
                } else {
                    maxFrequencyInterval = new FrequencyInterval(0, 0);
                }
            }
            YValueModel yValueModel = yValueModelManager.getYValueModel(plotIdentifier);
            yValueModel.setAvailableYMin(maxFrequencyInterval.getStart());
            yValueModel.setAvailableYMax(maxFrequencyInterval.getEnd());
        }
    }

    /**
     * Informs all RadioDataManagerListener of new received data.
     * 
     * @param data
     *            The data received
     * @param imageID
     *            The ID of the image for which data was received
     * @param downloadID
     *            The ID of the download batch the image is part of
     * @param dataSize
     *            The height and width of the data
     */
    private void fireDataforIDReceived(byte[] data, Long imageID, Long downloadID, Rectangle dataSize) {
        synchronized (downloadRequestData) {
            DownloadRequestData drd = downloadRequestData.get(downloadID);
            if (drd != null) {
                RadioImage image = drd.getRadioImages().get(imageID);
                if (image != null) {
                    synchronized (image) {
                        image.setLastDataSize(dataSize);
                        if (image.getVisibleImageFreqInterval() != null && image.getVisibleImageTimeInterval() != null) {
                            for (RadioDataManagerListener l : listeners) {
                                l.newDataForIDReceived(data, image.getVisibleImageTimeInterval(), image.getVisibleImageFreqInterval(), dataSize, downloadID, drd.getPlotIdentifier(), imageID);
                            }
                        }
                    }
                } else {
                    // Log.debug("The image was null");
                }
            } else {
                // Log.debug("Download request data was null");
            }
        }
    }

    /**
     * Informs the radio data manager listeners about new available data.
     * 
     * @param drd
     *            The available data
     * @param iD
     *            The download id
     */
    private void fireNewDataAvailable(DownloadRequestData drd, long iD) {
        for (RadioDataManagerListener l : listeners) {
            l.newDataAvailable(drd, iD);
        }
    }

    /**
     * Informs the radio data managers listeners about data that was removed.
     * 
     * @param drd
     *            The data that was removed
     */
    private void fireDownloadRequestDataRemoved(DownloadRequestData drd) {
        for (RadioDataManagerListener l : listeners) {
            l.downloadRequestDataRemoved(drd, drd.getDownloadID());
        }
    }

    /**
     * Informs the radio data manager listeners about the data visibility that
     * was changed.
     * 
     * @param drd
     *            The download request data for which the data visibility was
     *            changed.
     */
    private void fireDownloadRequestDataVisibilityChanged(DownloadRequestData drd) {
        for (RadioDataManagerListener l : listeners) {
            l.downloadRequestDataVisibilityChanged(drd, drd.getDownloadID());
        }
        lineDataSelectorModel.lineDataElementUpdated(drd);
    }

    /**
     * Informs the radio data manager listeners about the download request that
     * was answered.
     * 
     * @param timeInterval
     *            The time interval over which the request was answered
     * @param downloadID
     *            The download id for which the request was answered
     * @param plotIdentifier
     *            The identifier of the plot for which the request as answered
     */
    private void fireDownloadRequestAnswered(Interval<Date> timeInterval, long downloadID, String plotIdentifier) {
        for (RadioDataManagerListener l : listeners) {
            l.downloadRequestAnswered(timeInterval, downloadID, plotIdentifier);
        }
    }

    /**
     * Function handles a request configuration.
     * 
     * @param requestConfig
     *            The request configuration to be handled
     * @param xStart
     *            The start date of the currently visible time interval
     * @param xEnd
     *            The end date of the currently visible time interval
     * @param yStart
     *            The start value of the currently visible frequency interval
     * @param yEnd
     *            The end value of the currently visible frequency interval
     * @param plotIdentifier
     *            The identifier of the plot
     */
    private void handleRequestConfig(RequestConfig requestConfig, Date xStart, Date xEnd, double yStart, double yEnd, String plotIdentifier) {
        Log.trace("Request for data in interval " + requestConfig.getxStart() + " - " + requestConfig.getxEnd());
        Log.trace("Request for data in frequency interval " + requestConfig.getyStart() + " - " + requestConfig.getyEnd());
        if (requestConfig.getxEnd().getTime() - requestConfig.getxStart().getTime() > EVESettings.MAXIMUM_INTERVAL_RANGE_MILLI_SEC_REQ) {
            Log.trace("Interval too big");
            for (Long id : requestConfig.getIDs()) {
                fireIntervalTooBig(id, plotIdentifier);
            }
        } else {
            RadioImageCacheResult result = cache.getRadioImageCacheResultForInterval(requestConfig.getxStart(), requestConfig.getxEnd(), 24L * 60 * 60 * 1000, plotIdentifier);
            for (Long id : requestConfig.getIDs()) {
                DownloadRequestData drd = downloadRequestData.get(id);
                if (drd != null) {
                    downloader.requestAndOpenIntervals(result.getMissingInterval(), id, drd.getPlotIdentifier(), requestConfig.getxRatio(), requestConfig.getyRatio());
                } else {
                    Log.trace("drd is null");
                }
            }
            for (Long id : requestConfig.getIDs()) {
                DownloadRequestData drd = downloadRequestData.get(id);
                if (drd != null) {
                    fireClearSavedImages(id, result.getToRemove(), drd.getPlotIdentifier());
                    Log.trace("Size of available images : " + result.getAvailableData().size());
                    for (DownloadedJPXData jpxData : result.getAvailableData()) {
                        handleAvailableData(jpxData, xStart, xEnd, yStart, yEnd, plotIdentifier, drd);
                    }
                }
            }
        }
    }

    /**
     * Handles new available data send by the radio downloader
     * 
     * @param xStart
     *            The start time of the currently visible time interval
     * @param xEnd
     *            The end time of the currently visible time interval
     * @param yStart
     *            The start frequency of the currently visible frequency
     *            interval
     * @param yEnd
     *            The end frequency of the currently visible frequency interval
     * @param jpxData
     *            The downloaded jpx data that should be handled
     * @param plotIdentifier
     *            The identifier of the plot
     * @param drd
     *            The download request data
     * 
     */
    private void handleAvailableData(DownloadedJPXData jpxData, Date xStart, Date xEnd, double yStart, double yEnd, String plotIdentifier, DownloadRequestData drd) {
        RadioImage ri = drd.getRadioImages().get(jpxData.getImageID());
        if (ri != null) {
            synchronized (ri) {
                ri.setVisibleIntervals(xStart, xEnd, (int) Math.round(yStart), (int) Math.round(yEnd));
                if (ri.getVisibleImageFreqInterval() != null && ri.getVisibleImageTimeInterval() != null) {
                    Interval<Date> visibleDateInterval = ri.getVisibleImageTimeInterval();
                    FrequencyInterval visibleFrequencyInterval = ri.getVisibleImageFreqInterval();
                    if (!visibleDateInterval.getStart().equals(visibleDateInterval.getEnd())) {
                        Log.trace("Visible start time : " + visibleDateInterval.getStart().getTime() + " Date : " + visibleDateInterval.getStart());
                        Log.trace("Visible end time : " + visibleDateInterval.getEnd().getTime() + " Date : " + visibleDateInterval.getEnd());
                        Rectangle viewport = zoomManager.getAvailableSpaceForInterval(visibleDateInterval.getStart(), visibleDateInterval.getEnd(), visibleFrequencyInterval.getStart(), visibleFrequencyInterval.getEnd(), id, plotIdentifier);
                        View v = jpxData.getView();
                        JHVJP2View jp2View = v.getAdapter(JHVJP2View.class);
                        if (jp2View != null) {
                            jp2View.setViewport(new ViewportAdapter(new StaticViewport(new Vector2dInt(viewport.width, viewport.height))), new ChangeEvent());
                            Rectangle roi = ri.getROI();
                            Log.trace("*********************************************");
                            Log.trace("requested interval : " + xStart + " - " + xEnd + " , " + yStart + " - " + yEnd);
                            Log.trace("image id : " + ri.getRadioImageID());
                            Log.trace("image interval : " + ri.getTimeInterval().getStart() + " - " + ri.getTimeInterval().getEnd() + " , " + ri.getFreqInterval().getStart() + " - " + ri.getFreqInterval().getEnd());
                            Log.trace("visible interval : " + ri.getVisibleImageTimeInterval().getStart() + " - " + ri.getVisibleImageTimeInterval().getEnd() + " , " + ri.getVisibleImageFreqInterval().getStart() + " - " + ri.getVisibleImageFreqInterval().getEnd());
                            Log.trace("viewport[width-height] : " + viewport.width + " - " + viewport.height);
                            Log.trace("ROI [x0,y0,width,height] : " + roi.x + "," + roi.y + "," + roi.width + "," + roi.height);
                            Log.trace("*********************************************");
                            HelioviewerMetaData md = (HelioviewerMetaData) jp2View.getMetadata();
                            Double mpp = md.getUnitsPerPixel();
                            if (jp2View.setRegion(new RegionAdapter(new StaticRegion(roi.getX() * mpp, roi.getY() * mpp, new Vector2dDouble(roi.getWidth() * mpp, roi.getHeight() * mpp))), new ChangeEvent())) {
                                Log.trace("The region is changed");
                            } else {
                                Log.trace("The region has not changed send a data not changed for image id : " + ri.getRadioImageID());
                                if (ri.getLastDataSize() != null) {
                                    fireDataNotChanged(ri.getVisibleImageTimeInterval(), ri.getVisibleImageFreqInterval(), ri.getLastDataSize(), drd.getDownloadID(), drd.getPlotIdentifier(), ri.getRadioImageID());
                                } else {
                                    Log.trace("Last data size was null for radio image id : " + ri.getRadioImageID());
                                }
                            }
                        }
                    } else {
                        Log.trace("Start and end date of the visible interval were the same. No data requested");
                    }
                }
            }
        }
    }

    /**
     * Instructs the radio data manager listeners to clear the saved images in
     * the list.
     * 
     * @param downloadID
     *            The download identifier for which the images should be removed
     * @param toRemove
     *            The list of image identifiers that should be removed
     * @param plotIdentifier
     *            The plot identifier for which the image should be removed
     */
    private void fireClearSavedImages(Long downloadID, List<Long> toRemove, String plotIdentifier) {
        Log.trace("Clear images for downloadID " + downloadID + " and plotIdentifier " + plotIdentifier + " nr of elements to remove: " + toRemove.size());
        for (Long imageID : toRemove) {
            for (RadioDataManagerListener l : listeners) {
                l.clearAllSavedImagesForID(downloadID, imageID, plotIdentifier);
            }
        }

    }

    /**
     * Instructs the radio data managers about data that was not changed.
     * 
     * @param timeInterval
     *            The visible interval over which the data did not change
     * @param freqInterval
     *            The visible frequency interval over which the data did not
     *            change
     * @param rectangle
     *            The area available for the image
     * @param downloadID
     *            The download identifier for which the data did not change
     * @param plotIdentifier
     *            The plot identifier for which the data did not change
     * @param imageID
     *            The image identifier for which the data did not change
     */
    private void fireDataNotChanged(Interval<Date> timeInterval, FrequencyInterval freqInterval, Rectangle rectangle, Long downloadID, String plotIdentifier, long imageID) {
        List<Long> tempList = new ArrayList<Long>();
        tempList.add(downloadID);
        for (RadioDataManagerListener l : listeners) {
            l.dataNotChanged(timeInterval, freqInterval, rectangle, tempList, plotIdentifier, imageID);
        }

    }

    /**
     * Informs the radio data manager listeners about a request for a too big
     * interval
     * 
     * @param downloadID
     *            The download identifier for which the requested interval was
     *            too big
     * @param plotIdentifier
     *            The plot identifier for which the request was too big
     */
    private void fireIntervalTooBig(long downloadID, String plotIdentifier) {
        for (RadioDataManagerListener l : listeners) {
            l.intervalTooBig(downloadID, plotIdentifier);
        }
    }

    /**
     * Handles newly downloaded jpx-data.
     * 
     * @param djd
     *            The jpx-data to be handled
     * @param drd
     *            The corresponding download request data
     * @param sdf
     *            The used simple data format
     * @param downloadID
     *            The download identifier for which the data was downloaded
     * @param plotIdentifier
     *            The plot identifier for which the data was downloaded
     */
    private void handleDownloadedJPXData(DownloadedJPXData djd, SimpleDateFormat sdf, DownloadRequestData drd, Long downloadID, String plotIdentifier) {
        JHVJP2View jpxView = djd.getView().getAdapter(JHVJP2View.class);
        if (jpxView != null) {
            jpxView.setReaderMode(ReaderMode.ONLYFIREONCOMPLETE);
            JP2Image image = jpxView.getJP2Image();
            ResolutionSet rs = image.getResolutionSet();
            Interval<Integer> interval = image.getCompositionLayerRange();
            LineDataSelectorModel.getSingletonInstance().downloadStarted(drd);
            for (int i = interval.getStart(); i <= interval.getEnd(); i++) {
                try {
                    FrequencyInterval fi = new FrequencyInterval(Integer.parseInt(image.get("STARTFRQ", i)), Integer.parseInt(image.get("END-FREQ", i)));
                    Date start = null;
                    Date end = null;
                    try {
                        start = sdf.parse(image.get("DATE-OBS", i));
                        end = sdf.parse(image.get("DATE-END", i));
                    } catch (ParseException e) {
                        e.printStackTrace();
                        Log.error("Could not parse  " + image.get("DATE-OBS", i) + " or " + image.get("DATE-END", i));
                    }
                    List<ResolutionSetting> resolutionSettings = new ArrayList<ResolutionSetting>();
                    if (start != null && end != null) {
                        Double freqStart = Double.parseDouble(image.get("STARTFRQ", i));
                        Double freqEnd = Double.parseDouble(image.get("END-FREQ", i));
                        Interval<Date> dateInterval = new Interval<Date>(start, end);
                        for (int j = 0; j <= rs.getMaxResolutionLevels(); j++) {
                            ResolutionSetting tempResSet = new ResolutionSetting((1.0 * (end.getTime() - start.getTime()) / rs.getResolutionLevel(j).getResolutionBounds().width), ((freqEnd - freqStart) / rs.getResolutionLevel(j).getResolutionBounds().height), j, rs.getResolutionLevel(j).getResolutionBounds().width, rs.getResolutionLevel(j).getResolutionBounds().height, rs.getResolutionLevel(j).getZoomLevel());
                            resolutionSettings.add(tempResSet);
                        }
                        int highestLevel = -1;
                        ResolutionSetting lastUsedResolutionSetting = null;
                        for (ResolutionSetting rst : resolutionSettings) {
                            if (rst.getResolutionLevel() > highestLevel) {
                                highestLevel = rst.getResolutionLevel();
                                lastUsedResolutionSetting = rst;
                            }
                        }
                        jpxView.setViewport(new ViewportAdapter(new StaticViewport(lastUsedResolutionSetting.getVec2dIntRepresentation())), new ChangeEvent());
                        RadioImage tempRs = new RadioImage(djd, downloadID, djd.getImageID(), dateInterval, fi, i, rs, resolutionSettings, plotIdentifier, true);
                        tempRs.setLastUsedResolutionSetting(lastUsedResolutionSetting);
                        drd.addRadioImage(tempRs);
                    } else {
                        Log.error("Start and/or stop is null");
                    }
                } catch (IOException e) {
                    Log.error("Some of the metadata could not be read aborting...");
                    return;
                }
            }
        }
    }

    /**
     * Handles the download of additional jpx-data.
     * 
     * @param ratioY
     *            The x-ratio of time per pixel
     * @param ratioX
     *            The y-ratio of frequency per pixel
     * @param djd
     *            The additional downloaded jpx-data
     * @param drd
     *            The corresponding download request data
     * @param downloadID
     *            The download identifier for which additional data was
     *            downloaded
     * @param sdf
     *            The used simple data format
     * @param plotIdentifier
     *            The plot identifier for which new additional data was
     *            downloaded
     * 
     */
    private void handleAdditionalJPXData(DownloadedJPXData djd, double ratioX, double ratioY, Long downloadID, DownloadRequestData drd, SimpleDateFormat sdf, String plotIdentifier) {
        JHVJP2View jpxView = djd.getView().getAdapter(JHVJP2View.class);
        if (jpxView != null) {
            jpxView.setReaderMode(ReaderMode.ONLYFIREONCOMPLETE);
            JP2Image image = jpxView.getJP2Image();
            ResolutionSet rs = image.getResolutionSet();
            Interval<Integer> interval = image.getCompositionLayerRange();
            LineDataSelectorModel.getSingletonInstance().downloadStarted(drd);
            for (int i = interval.getStart(); i <= interval.getEnd(); i++) {
                try {
                    FrequencyInterval fi = new FrequencyInterval(Integer.parseInt(image.get("STARTFRQ", i)), Integer.parseInt(image.get("END-FREQ", i)));
                    Date start = null;
                    Date end = null;
                    try {
                        start = sdf.parse(image.get("DATE-OBS", i));
                        end = sdf.parse(image.get("DATE-END", i));
                    } catch (ParseException e) {
                        e.printStackTrace();
                        Log.error("Could not parse  " + image.get("DATE-OBS", i) + " or " + image.get("DATE-END", i));
                    }
                    List<ResolutionSetting> resolutionSettings = new ArrayList<ResolutionSetting>();
                    if (start != null && end != null) {
                        Double freqStart = Double.parseDouble(image.get("STARTFRQ", i));
                        Double freqEnd = Double.parseDouble(image.get("END-FREQ", i));
                        Interval<Date> dateInterval = new Interval<Date>(start, end);
                        for (int j = 0; j <= rs.getMaxResolutionLevels(); j++) {
                            ResolutionSetting tempResSet = new ResolutionSetting((1.0 * (end.getTime() - start.getTime()) / rs.getResolutionLevel(j).getResolutionBounds().width), ((freqEnd - freqStart) / rs.getResolutionLevel(j).getResolutionBounds().height), j, rs.getResolutionLevel(j).getResolutionBounds().width, rs.getResolutionLevel(j).getResolutionBounds().height, rs.getResolutionLevel(j).getZoomLevel());
                            resolutionSettings.add(tempResSet);
                        }
                        RadioImage tempRs = new RadioImage(djd, downloadID, djd.getImageID(), dateInterval, fi, i, rs, resolutionSettings, plotIdentifier, true);
                        ResolutionSetting lastUsedResolutionSetting = tempRs.defineBestResolutionSetting(ratioX, ratioY);
                        jpxView.setViewport(new ViewportAdapter(new StaticViewport(lastUsedResolutionSetting.getVec2dIntRepresentation())), new ChangeEvent());
                        tempRs.setLastUsedResolutionSetting(lastUsedResolutionSetting);
                        drd.addRadioImage(tempRs);
                    } else {
                        Log.error("Start and/or stop is null");
                    }
                } catch (IOException e) {
                    return;
                }
            }
        }
    }

    /**
     * Informs radio data manager listeners about interval with no data that
     * where reseive.
     * 
     * @param noDataList
     *            A list with interval for which no data was received
     * @param downloadID
     *            The download identifier for which intervals with no data was
     *            received
     * @param plotIdentifier
     *            The plot identifier for which intervals with no data were
     *            received
     */
    private void fireNoDataIntervalsReceived(List<Interval<Date>> noDataList, Long downloadID, String plotIdentifier) {
        for (RadioDataManagerListener l : listeners) {
            l.noDataInterval(noDataList, downloadID, plotIdentifier);
        }
    }

}
