package org.helioviewer.plugins.eveplugin.radio.data;

import java.awt.Rectangle;
import java.util.Date;
import java.util.List;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.plugins.eveplugin.radio.model.ResolutionSetting;

public class RadioImage {
    private Interval<Date> imageTimeInterval;
    private FrequencyInterval imageFreqInterval;
    private Interval<Date> visibleImageTimeInterval;
    private FrequencyInterval visibleImageFreqInterval;
    private int frameInJPX;
    private ResolutionSet resolutioSet;
    private List<ResolutionSetting> resolutionSettings;
    private long downloadID;
    private ResolutionSetting lastUsedResolutionSetting;
    private long radioImageID;
    private boolean isDownloading;
    private Rectangle lastDataSize;
    

    public RadioImage(DownloadedJPXData jpxData, long downloadID, Long radioImageID, Interval<Date> timeInterval, FrequencyInterval freqInterval, int frameInJPX, ResolutionSet rs, List<ResolutionSetting> resolutionSettings, String plotIdentifier, boolean isDownloading) {
        super();
        this.downloadID = downloadID;
        this.imageTimeInterval = timeInterval;
        this.imageFreqInterval = freqInterval;
        this.visibleImageFreqInterval = freqInterval;
        this.visibleImageTimeInterval = timeInterval;
        this.frameInJPX = frameInJPX;
        this.resolutioSet = rs;
        this.resolutionSettings = resolutionSettings;
        this.radioImageID = radioImageID;
        this.isDownloading = isDownloading;
    }

    
    /**
     * Gives the size of the latest data received
     * 
     * @return  Rectangle with the size of the latest received data for this RadioImage
     */
    public Rectangle getLastDataSize() {
        Log.debug("get last data size for image id : " + radioImageID);
        return lastDataSize;
    }

    /**
     * Sets the size of the latest received data. 
     * 
     * @param lastDataSize The size of the last data download
     */
    public void setLastDataSize(Rectangle lastDataSize) {
        Log.debug("Set last data size for image id : " + radioImageID);
        this.lastDataSize = lastDataSize;
    }

    /**
     * Indicates if this radio image is downloading.
     * 
     * @return  True if the radio image is downloading, false if not
     */
    public boolean isDownloading() {
        
        return isDownloading;
    }

    /**
     * Sets whether the radio image is downloading
     * 
     * @param isDownloading True if the radio image is downloading, false if not
     */
    public void setDownloading(boolean isDownloading) {
        this.isDownloading = isDownloading;
    }

    public long getRadioImageID() {
        return radioImageID;
    }

    public long getDownloadID() {
        return downloadID;
    }

    public void setDownloadID(long iD) {
        downloadID = iD;
    }

    public ResolutionSetting getLastUsedResolutionSetting() {
        return lastUsedResolutionSetting;
    }

    public void setLastUsedResolutionSetting(ResolutionSetting resolutionSetting) {
        this.lastUsedResolutionSetting = resolutionSetting;
    }

    public Interval<Date> getTimeInterval() {
        return imageTimeInterval;
    }
       
    public Interval<Date> getVisibleImageTimeInterval() {
        return visibleImageTimeInterval;
    }

    public FrequencyInterval getVisibleImageFreqInterval() {
        return visibleImageFreqInterval;
    }

    public void setTimeInterval(Interval<Date> timeInterval) {
        this.imageTimeInterval = timeInterval;
    }

    public FrequencyInterval getFreqInterval() {
        return imageFreqInterval;
    }

    public void setFreqInterval(FrequencyInterval freqInterval) {
        this.imageFreqInterval = freqInterval;
    }

    public int getFrameInJPX() {
        return frameInJPX;
    }

    public void setFrameInJPX(int frameInJPX) {
        this.frameInJPX = frameInJPX;
    }

    public ResolutionSet getResolutioSet() {
        return resolutioSet;
    }

    public void setResolutioSet(ResolutionSet resolutioSet) {
        this.resolutioSet = resolutioSet;
    }

    public ResolutionSetting defineBestResolutionSetting(double ratioX, double ratioY) {
        ResolutionSetting currentBest = null;
        int highestLevel = 0;
        for (ResolutionSetting rs : resolutionSettings) {
            if (rs.getxRatio() < ratioX || rs.getyRatio() < ratioY) {
                if (rs.getResolutionLevel() > highestLevel) {
                    highestLevel = rs.getResolutionLevel();
                    currentBest = rs;
                }
            }
            if (rs.getResolutionLevel() == 0 && currentBest == null) {
                currentBest = rs;
            }
        }
        return currentBest;
    }

    public boolean withinInterval(Interval<Date> intervalToBeIn, FrequencyInterval freqIntervalToBeIn) {
        return intervalToBeIn.overlapsInclusive(imageTimeInterval) && freqIntervalToBeIn.overlaps(imageFreqInterval);
    }
    
    
    /**
     * Defines the visible interval for this image based on the given visible time and frequency. If the given 
     * visible interval start or end (both time and frequency) lies within the time and frequency interval of this image. 
     * The part of this image that is visible is defined.  
     * 
     * @param visibleXStart     The start time of the visible time interval
     * @param visibleXEnd       The end time of the visible time interval
     * @param visibleYStart     The start frequency of the visible interval 
     * @param visibleYEnd       The end frequency of the visible interval
     */
    public void setVisibleIntervals(Date visibleXStart, Date visibleXEnd, int visibleYStart, int visibleYEnd){
        if(imageTimeInterval.containsPointInclusive(visibleXStart) || imageTimeInterval.containsPointInclusive(visibleXEnd)){
            Date tempStartX = new Date(imageTimeInterval.squeeze(visibleXStart).getTime());
            Date tempEndX = new Date(imageTimeInterval.squeeze(visibleXEnd).getTime());
            this.visibleImageTimeInterval = new Interval<Date>(tempStartX,tempEndX);
            Log.debug("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
            Log.debug("Image interval : ");
            Log.debug("Start x : " + imageTimeInterval.getStart() + " in milliseconds : " +  imageTimeInterval.getStart().getTime());
            Log.debug("End x : " + imageTimeInterval.getEnd() + " in milliseconds : " +  imageTimeInterval.getEnd().getTime());
            Log.debug("Requested interval : ");
            Log.debug("Start x : " + visibleXStart + " in milliseconds : " + visibleXStart.getTime());
            Log.debug("End x : " + visibleXEnd + " in milliseconds : " + visibleXEnd.getTime());
            Log.debug("Resulting visible time interval start : "+ visibleImageTimeInterval.getStart() + " in milliseconds : " + visibleImageTimeInterval.getStart().getTime());
            Log.debug("Resulting visible time interval end : "+ visibleImageTimeInterval.getEnd() + " in milliseconds : " + visibleImageTimeInterval.getEnd().getTime());
            Log.debug("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        }else{
            this.visibleImageTimeInterval = null;
        }
        if(imageFreqInterval.containsInclusive(visibleYStart) || imageFreqInterval.containsInclusive(visibleYEnd)){
            int tempStartY = imageFreqInterval.squeeze(visibleYStart);
            int tempEndY = imageFreqInterval.squeeze(visibleYEnd);
            this.visibleImageFreqInterval = new FrequencyInterval(tempStartY, tempEndY);
        }else{
            this.visibleImageFreqInterval = null;
        }
        
    }
    
    /**
     * Defines the region of interest based on the visible time and frequency interval. The region of interest is given in coordinates 
     * in the highest resolution level. 
     * 
     * @return  A Rectangle containing the region of interest of the visible time and frequency interface.
     */
    public Rectangle getROI(){
        if(visibleImageFreqInterval != null && visibleImageTimeInterval != null){
            int maxImageWidth = resolutioSet.getResolutionLevel(0).getResolutionBounds().width;
            int maxImageHeight = resolutioSet.getResolutionLevel(0).getResolutionBounds().height;
            long imageTimesize = imageTimeInterval.getEnd().getTime() - imageTimeInterval.getStart().getTime();
            int imageFrequencySize = imageFreqInterval.getEnd() - imageFreqInterval.getStart();
            double timePerPix = 1.0*imageTimesize/maxImageWidth;
            double freqPerPix = 1.0*imageFrequencySize/maxImageHeight;
            
            int x0 =(int)Math.round((visibleImageTimeInterval.getStart().getTime()-imageTimeInterval.getStart().getTime())/timePerPix);
            int y0 = (int)Math.round((visibleImageFreqInterval.getStart()-imageFreqInterval.getStart())/freqPerPix);
            int width =(int) Math.round((visibleImageTimeInterval.getEnd().getTime()-visibleImageTimeInterval.getStart().getTime())/timePerPix);
            int height = (int)Math.round((visibleImageFreqInterval.getEnd()-visibleImageFreqInterval.getStart())/freqPerPix);
            return new Rectangle(x0, y0, width, height);
        }else{
            return null;
        }
    }
}
