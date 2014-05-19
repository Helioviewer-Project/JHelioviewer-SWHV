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
import org.helioviewer.plugins.eveplugin.radio.model.ZoomManager;
import org.helioviewer.plugins.eveplugin.settings.EVESettings;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.region.RegionAdapter;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.ImageInfoView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View.ReaderMode;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.jp2view.JP2Image;
import org.helioviewer.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.viewmodel.viewport.StaticViewport;
import org.helioviewer.viewmodel.viewport.ViewportAdapter;

public class RadioDataManager implements RadioDownloaderListener {
    private static RadioDataManager instance;
    private List<RadioDataManagerListener> listeners;
    private Map<Long, DownloadRequestData> downloadRequestData;
    private FrequencyInterval maxFrequencyInterval;
    private RadioDownloader downloader;
    private RequestForDataBuffer requestBuffer;
    private LineDataSelectorModel lineDataSelectorModel;
    private RadioImageCache cache;
    private ZoomManager zoomManager;
    private long id = 0;
    private EVEState eveState;

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
    }

    public static RadioDataManager getSingletonInstance() {
        if (instance == null) {
            instance = new RadioDataManager();
        }
        return instance;
    }

    private void defineMaxBounds(Long ID) {
        synchronized (downloadRequestData) {
            DownloadRequestData drd = this.downloadRequestData.get(ID);
            if (drd != null) {
                Map<Long, RadioImage> radioImages = drd.getRadioImages();
                if (!radioImages.isEmpty()) {
                    Date localMinDate = null;
                    Date localMaxDate = null;
                    int localMinFrequency = -1;
                    int localMaxFrequency = -1;
                    boolean first = true;
                    for (RadioImage image : radioImages.values()) {
                        if (first) {
                            localMinDate = image.getTimeInterval().getStart();
                            localMaxDate = image.getTimeInterval().getEnd();
                            localMinFrequency = image.getFreqInterval().getStart();
                            localMaxFrequency = image.getFreqInterval().getEnd();
                            first = false;
                        } else {
                            if (image.getTimeInterval().getStart().getTime() < localMinDate.getTime()) {
                                localMinDate = image.getTimeInterval().getStart();
                            }
                            if (image.getTimeInterval().getEnd().getTime() > localMaxDate.getTime()) {
                                localMaxDate = image.getTimeInterval().getEnd();
                            }
                            if (image.getFreqInterval().getStart() < localMinFrequency) {
                                localMinFrequency = image.getFreqInterval().getStart();
                            }
                            if (image.getFreqInterval().getEnd() > localMaxFrequency) {
                                localMaxFrequency = image.getFreqInterval().getEnd();
                            }
                        }
                    }
                    this.maxFrequencyInterval = new FrequencyInterval(localMinFrequency, localMaxFrequency);
                }
            }
        }
    }

    public void addRadioManagerListener(RadioDataManagerListener l) {
        listeners.add(l);
    }

    public void removeRadioManagerListener(RadioDataManagerListener l) {
        listeners.remove(l);
    }

    /**
     * Inform the RadioDataManager about the new data that was received. The RadioDataManagerListeners will be informed about 
     * the new received data.
     * 
     * 
     * @param data          The data received
     * @param imageID       The ID of the image for which data was received
     * @param downloadID    The ID of the download batch the image is part of
     * @param dataSize      The height and width of the data
     */
    public synchronized void dataForIDReceived(byte[] data, Long imageID, Long downloadID, Rectangle dataSize) {
        fireDataforIDReceived(data, imageID, downloadID, dataSize);
    }
    
    /**
     * Informs all RadioDataManagerListener of new received data.
     * 
     * @param data          The data received
     * @param imageID       The ID of the image for which data was received
     * @param downloadID    The ID of the download batch the image is part of
     * @param dataSize      The height and width of the data
     */
    private void fireDataforIDReceived(byte[] data, Long imageID, Long downloadID, Rectangle dataSize) {
        synchronized (downloadRequestData) {
            DownloadRequestData drd = downloadRequestData.get(downloadID);
            if (drd != null) {
                RadioImage image = drd.getRadioImages().get(imageID);                
                if (image != null) {
                    synchronized (image) {
                        image.setLastDataSize(dataSize);
                        if (image.getVisibleImageFreqInterval() != null && image.getVisibleImageTimeInterval() != null){
                            for (RadioDataManagerListener l : listeners) {
                                //l.newDataForIDReceived(data, image.getTimeInterval(), image.getFreqInterval(), image.getLastUsedResolutionSetting().getRectangleRepresentation(), downloadID, drd.getPlotIdentifier(), imageID);
                                l.newDataForIDReceived(data, image.getVisibleImageTimeInterval(), image.getVisibleImageFreqInterval(), dataSize, downloadID, drd.getPlotIdentifier(), imageID);
                            }
                        }
                    }
                } else {
                    //Log.debug("The image was null");
                }                               
            } else {
                //Log.debug("Download request data was null");
            }
        }
    }

    
    
    @Override
    public void newImageViewDownloaded(ImageInfoView view, Date requestedStartTime, Date requestedEndTime, long ID, String identifier) {
        synchronized (downloadRequestData) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                JHVJPXView jpxView = view.getAdapter(JHVJPXView.class);
                if (jpxView != null) {
                    JP2Image image = jpxView.getJP2Image();
                    ResolutionSet rs = image.getResolutionSet();
                    Interval<Integer> interval = image.getCompositionLayerRange();
                    DownloadRequestData drd = new DownloadRequestData(ID, identifier);
                    drd.setDownloading(true);
                    lineDataSelectorModel.addLineData(drd);
                    LineDataSelectorModel.getSingletonInstance().downloadStarted(drd);
                    for (int i = interval.getStart(); i <= interval.getEnd(); i++) {
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
                            for (int j = 0; j < rs.getMaxResolutionLevels(); j++) {
                                ResolutionSetting tempResSet = new ResolutionSetting((1.0 * (end.getTime() - start.getTime()) / rs.getResolutionLevel(j).getResolutionBounds().width), ((freqEnd - freqStart) / rs.getResolutionLevel(j).getResolutionBounds().height), j, rs.getResolutionLevel(j).getResolutionBounds().width, rs.getResolutionLevel(j).getResolutionBounds().height, rs.getResolutionLevel(j).getZoomLevel());
                                resolutionSettings.add(tempResSet);
                            }
                            RadioImage tempRs = new RadioImage(null, ID, Math.round(1000000 * Math.random()), dateInterval, fi, i, rs, resolutionSettings, identifier, true);
                            drd.addRadioImage(tempRs);
                        } else {
                            Log.error("Start and/or stop is null");
                        }                    }
                    this.downloadRequestData.put(ID, drd);
                    defineMaxBounds(ID);
                    fireNewDataAvailable(drd, ID);
                    fireDownloadRequestAnswered(maxFrequencyInterval, new Interval<Date>(requestedStartTime, requestedEndTime), ID, identifier);
                } else {
                    JHVJP2View jp2View = view.getAdapter(JHVJP2View.class);
                    JP2Image image = jp2View.getJP2Image();
                    ResolutionSet rs = image.getResolutionSet();
                    Interval<Integer> interval = image.getCompositionLayerRange();
                    DownloadRequestData drd = new DownloadRequestData(ID, identifier);
                    drd.setDownloading(true);
                    lineDataSelectorModel.addLineData(drd);
                    LineDataSelectorModel.getSingletonInstance().downloadStarted(drd);
                    for (int i = interval.getStart(); i <= interval.getEnd(); i++) {
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
                            for (int j = 0; j < rs.getMaxResolutionLevels(); j++) {
                                ResolutionSetting tempResSet = new ResolutionSetting((1.0 * (end.getTime() - start.getTime()) / rs.getResolutionLevel(j).getResolutionBounds().width), ((freqEnd - freqStart) / rs.getResolutionLevel(j).getResolutionBounds().height), j, rs.getResolutionLevel(j).getResolutionBounds().width, rs.getResolutionLevel(j).getResolutionBounds().height, rs.getResolutionLevel(j).getZoomLevel());
                                resolutionSettings.add(tempResSet);
                            }
                            RadioImage tempRs = new RadioImage(null, ID, Math.round(1000000 * Math.random()), dateInterval, fi, i, rs, resolutionSettings, identifier, true);
                            drd.addRadioImage(tempRs);
                        } else {
                            Log.error("Start and/or stop is null");
                        }
                    }
                    this.downloadRequestData.put(ID, drd);
                    defineMaxBounds(ID);
                    fireNewDataAvailable(drd, ID);
                    fireDownloadRequestAnswered(maxFrequencyInterval, new Interval<Date>(requestedStartTime, requestedEndTime), ID, identifier);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void fireNewDataAvailable(DownloadRequestData drd, long iD) {
        for (RadioDataManagerListener l : listeners) {
            l.newDataAvailable(drd, iD);
        }
    }

    public void removeDownloadRequestData(DownloadRequestData drd) {
        this.downloadRequestData.remove(drd.getDownloadID());
        lineDataSelectorModel.removeLineData(drd);
        for (Long imageID : drd.getRadioImages().keySet()) {
            cache.remove(imageID, drd.getPlotIdentifier());
        }
        fireDownloadRequestDataRemoved(drd);
    }

    private void fireDownloadRequestDataRemoved(DownloadRequestData drd) {
        for (RadioDataManagerListener l : listeners) {
            l.downloadRequestDataRemoved(drd, drd.getDownloadID());
        }
    }

    public void downloadRequestDataVisibilityChanged(DownloadRequestData drd) {
        fireDownloadRequestDataVisibilityChanged(drd);
    }

    private void fireDownloadRequestDataVisibilityChanged(DownloadRequestData drd) {
        for (RadioDataManagerListener l : listeners) {
            l.downloadRequestDataVisibilityChanged(drd, drd.getDownloadID());
        }
        lineDataSelectorModel.lineDataElementUpdated(drd);
    }

    private void fireDownloadRequestAnswered(FrequencyInterval freqInterval, Interval<Date> timeInterval, long ID, String identifier) {
        for (RadioDataManagerListener l : listeners) {
            l.downloadRequestAnswered(freqInterval, timeInterval, ID, identifier);
        }
    }

    public void requestForData(Date xStart, Date xEnd, double yStart, double yEnd, double xRatio, double yRatio, List<Long> iDs, String plotIdentifier) {
        Long start = System.currentTimeMillis();
        Log.info("Request for data : " + id + " time " + start);
        if (!eveState.isMouseTimeIntervalDragging() && !eveState.isMouseValueIntervalDragging()) {
            Log.info("mouse is not dragged");
            if (!requestBuffer.hasData()) {
                requestBuffer.addRequestConfig(new RequestConfig(xStart, xEnd, yStart, yEnd, xRatio, yRatio, iDs));
                while (requestBuffer.hasData()) {
                    RequestConfig requestConfig = requestBuffer.getData();
                    synchronized (downloadRequestData) {
                        Log.info("Request for data in interval " + requestConfig.getxStart() + " - " + requestConfig.getxEnd());
                        Log.info("Request for data in frequency interval "+ requestConfig.getyStart() + " - " + requestConfig.getyEnd());
                        if (requestConfig.getxEnd().getTime() - requestConfig.getxStart().getTime() > EVESettings.MAXIMUM_INTERVAL_RANGE_MILLI_SEC_REQ) {
                            Log.info("Interval too big");
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
                                    Log.info("drd is null");
                                }
                            }
                            for (Long id : requestConfig.getIDs()) {
                                DownloadRequestData drd = downloadRequestData.get(id);
                                if (drd != null) {
                                    fireClearSavedImages(id, result.getToRemove(), drd.getPlotIdentifier());
                                    Log.info("Size of available images : "+ result.getAvailableData().size());
                                    for (DownloadedJPXData jpxData : result.getAvailableData()) {
                                        RadioImage ri = drd.getRadioImages().get(jpxData.getImageID());
                                        if (ri != null) {
                                            synchronized (ri) {                                       
                                                ri.setVisibleIntervals(xStart, xEnd, (int)Math.round(yStart),(int)Math.round(yEnd));
                                                if (ri.getVisibleImageFreqInterval() != null && ri.getVisibleImageTimeInterval() != null) {
                                                    Interval<Date> visibleDateInterval = ri.getVisibleImageTimeInterval();
                                                    FrequencyInterval visibleFrequencyInterval = ri.getVisibleImageFreqInterval();
                                                    if (!visibleDateInterval.getStart().equals(visibleDateInterval.getEnd())) {
                                                        Log.info("Visible start time : "+ visibleDateInterval.getStart().getTime() + " Date : "+ visibleDateInterval.getStart());
                                                        Log.info("Visible end time : " + visibleDateInterval.getEnd().getTime() + " Date : "+ visibleDateInterval.getEnd());
                                                        Rectangle viewport = zoomManager.getAvailableSpaceForInterval(
                                                                visibleDateInterval.getStart(),
                                                                visibleDateInterval.getEnd(), 
                                                                visibleFrequencyInterval.getStart(), 
                                                                visibleFrequencyInterval.getEnd(), id, plotIdentifier);
                                                        View v = jpxData.getView();
                                                        JHVJP2View jp2View = v.getAdapter(JHVJP2View.class);
                                                        if (jp2View != null) {
                                                            jp2View.setViewport(new ViewportAdapter(new StaticViewport(new Vector2dInt(viewport.width, viewport.height))), new ChangeEvent());
                                                            Rectangle roi = ri.getROI();
                                                            Log.info("*********************************************");
                                                            Log.info("requested interval : " + xStart + " - " + xEnd + " , " + yStart + " - " + yEnd);
                                                            Log.info("image id : "+ ri.getRadioImageID());
                                                            Log.info("image interval : " + ri.getTimeInterval().getStart() + " - " + ri.getTimeInterval().getEnd() + " , " + ri.getFreqInterval().getStart() + " - " + ri.getFreqInterval().getEnd());
                                                            Log.info("visible interval : " + ri.getVisibleImageTimeInterval().getStart() + " - " + ri.getVisibleImageTimeInterval().getEnd() + " , " + ri.getVisibleImageFreqInterval().getStart() + " - " + ri.getVisibleImageFreqInterval().getEnd());
                                                            Log.info("viewport[width-height] : " + viewport.width + " - " + viewport.height);
                                                            Log.info("ROI [x0,y0,width,height] : " + roi.x + "," + roi.y + "," + roi.width + "," + roi.height);
                                                            Log.info("*********************************************");
                                                            HelioviewerMetaData md = (HelioviewerMetaData)jp2View.getMetadata();
                                                            Double mpp = md.getUnitsPerPixel();
                                                            if(jp2View.setRegion(new RegionAdapter(new StaticRegion(roi.getX()*mpp, roi.getY()*mpp,
                                                                    new Vector2dDouble(roi.getWidth()*mpp, roi.getHeight()*mpp))), new ChangeEvent())){
                                                                Log.info("The region is changed");
                                                            }else{
                                                                Log.info("The region has not changed send a data not changed for image id : " + ri.getRadioImageID());
                                                                if(ri.getLastDataSize() != null){//can be null if a previous thread didn't finish yet
                                                                    fireDataNotChanged(ri.getVisibleImageTimeInterval(), ri.getVisibleImageFreqInterval(), ri.getLastDataSize(), id, drd.getPlotIdentifier(), ri.getRadioImageID());
                                                                }else{
                                                                    Log.info("Last data size was null for radio image id : " + ri.getRadioImageID());
                                                                }
                                                            }
            
                                                        }
                                                    }else{
                                                        Log.info("Start and end date of the visible interval were the same. No data requested");
                                                    }
                                                }
                                            }
                                            /*ResolutionSetting rs = ri.defineBestResolutionSetting(xRatio, yRatio);
                                            if (rs != ri.getLastUsedResolutionSetting()) {
                                                Log.debug("Other resolution setting: update the viewport for " + jpxData.getImageID());
                                                Log.debug("radio image last resolutionsetting : " + ri.getLastUsedResolutionSetting());
                                                Log.debug("new resolution setting : " + rs);
                                                View v = jpxData.getView();
                                                JHVJP2View jp2View = v.getAdapter(JHVJP2View.class);
                                                if (jp2View != null) {
                                                    jp2View.setViewport(new ViewportAdapter(new StaticViewport(rs.getVec2dIntRepresentation())), new ChangeEvent());
                                                    ri.setLastUsedResolutionSetting(rs);
                                                }
                                                
                                            } else {
                                                Log.debug("Same resolution setting don't update viewport");
                                                fireDataNotChanged(ri.getTimeInterval(), ri.getFreqInterval(), new Rectangle(rs.getWidth(), rs.getHeight()), id, drd.getPlotIdentifier(), ri.getRadioImageID());
                                            }
                                            */
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Log.info("Add request to buffer");
                requestBuffer.addRequestConfig(new RequestConfig(xStart, xEnd, yStart, yEnd, xRatio, yRatio, iDs));
            }
        }else{
            Log.info("Mouse is dragged");
        }
        long end  = System.currentTimeMillis();
        Log.info("Finished request for data id: " + id + " time : " + end +  " in " + (end-start) + " milliseconds");
        id++;
    }

    private void fireClearSavedImages(Long downloadID, List<Long> toRemove, String plotIdentifier) {
        Log.info("Clear images for downloadID " + downloadID + " and plotIdentifier " + plotIdentifier + " nr of elements to remove: " + toRemove.size());
        for (Long imageID : toRemove) {
            for (RadioDataManagerListener l : listeners) {
                l.clearAllSavedImagesForID(downloadID, imageID, plotIdentifier);
            }
        }

    }

    private void fireDataNotChanged(Interval<Date> timeInterval, FrequencyInterval freqInterval, Rectangle rectangle, Long id, String plotIdentifier, long imageID) {
        List<Long> tempList = new ArrayList<Long>();
        tempList.add(id);
        for (RadioDataManagerListener l : listeners) {
            l.dataNotChanged(timeInterval, freqInterval, rectangle, tempList, plotIdentifier, imageID);
        }

    }
    
    @Override
    public void intervalTooBig(Date requestedStartTime, Date requestedEndTime, long ID, String identifier) {
        DownloadRequestData drd = new DownloadRequestData(ID, identifier);
        downloadRequestData.put(ID, drd);
        lineDataSelectorModel.addLineData(drd);
        fireIntervalTooBig(ID, identifier);
        fireNewDataAvailable(drd, ID);
        fireDownloadRequestAnswered(new FrequencyInterval(20, 400), new Interval<Date>(requestedStartTime, requestedEndTime), ID, identifier);

    }

    private void fireIntervalTooBig(long ID, String plotIdentifier) {
        for (RadioDataManagerListener l : listeners) {
            l.intervalTooBig(ID, plotIdentifier);
        }

    }

    @Override
    public void newJPXFilesDownloaded(List<DownloadedJPXData> jpxFiles, Date requestedStartTime, Date requestedEndTime, Long downloadID, String plotIdentifier) {
        Log.info("Init the download request data in radio data manager");
        synchronized (downloadRequestData) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            DownloadRequestData drd = new DownloadRequestData(downloadID, plotIdentifier);
            drd.setDownloading(true);
            lineDataSelectorModel.addLineData(drd);
            if (!jpxFiles.isEmpty()) {
                for (DownloadedJPXData djd : jpxFiles) {
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
            }
            this.downloadRequestData.put(downloadID, drd);
            defineMaxBounds(downloadID);
            fireNewDataAvailable(drd, downloadID);
            fireDownloadRequestAnswered(maxFrequencyInterval, new Interval<Date>(requestedStartTime, requestedEndTime), downloadID, plotIdentifier);
        }
    }

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

    @Override
    public void newAdditionalDataDownloaded(List<DownloadedJPXData> jpxFiles, Long downloadID, String plotIdentifier, double ratioX, double ratioY) {
        synchronized (downloadRequestData) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            DownloadRequestData drd = downloadRequestData.get(downloadID);
            drd.setDownloading(true);
            lineDataSelectorModel.downloadStarted(drd);
            for (DownloadedJPXData djd : jpxFiles) {
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
            this.downloadRequestData.put(downloadID, drd);
            defineMaxBounds(downloadID);
            fireNewDataAvailable(drd, downloadID);
            fireAdditionalDownloadRequestAnswered(downloadID);
        }
    }

    private void fireAdditionalDownloadRequestAnswered(Long downloadID) {
        for (RadioDataManagerListener l : listeners) {
            l.additionDownloadRequestAnswered(downloadID);
        }
    }
}
