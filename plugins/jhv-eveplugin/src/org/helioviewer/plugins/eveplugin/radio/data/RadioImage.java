package org.helioviewer.plugins.eveplugin.radio.data;

import java.util.Date;
import java.util.List;

import org.helioviewer.base.math.Interval;
import org.helioviewer.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.plugins.eveplugin.radio.model.ResolutionSetting;

public class RadioImage {
    private Interval<Date> timeInterval;
    private FrequencyInterval freqInterval;
    private int frameInJPX;
    private ResolutionSet resolutioSet;
    private List<ResolutionSetting> resolutionSettings;
    private long downloadID;
    private ResolutionSetting lastUsedResolutionSetting;
    private long radioImageID;
    private boolean isDownloading;

    public RadioImage(DownloadedJPXData jpxData, long downloadID, Long radioImageID, Interval<Date> timeInterval, FrequencyInterval freqInterval, int frameInJPX, ResolutionSet rs, List<ResolutionSetting> resolutionSettings, String plotIdentifier, boolean isDownloading) {
        super();
        this.downloadID = downloadID;
        this.timeInterval = timeInterval;
        this.freqInterval = freqInterval;
        this.frameInJPX = frameInJPX;
        this.resolutioSet = rs;
        this.resolutionSettings = resolutionSettings;
        this.radioImageID = radioImageID;
        this.isDownloading = isDownloading;
    }

    public boolean isDownloading() {
        return isDownloading;
    }

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
        return timeInterval;
    }

    public void setTimeInterval(Interval<Date> timeInterval) {
        this.timeInterval = timeInterval;
    }

    public FrequencyInterval getFreqInterval() {
        return freqInterval;
    }

    public void setFreqInterval(FrequencyInterval freqInterval) {
        this.freqInterval = freqInterval;
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
        return intervalToBeIn.overlapsInclusive(timeInterval) && freqIntervalToBeIn.overlaps(freqInterval);
    }
}
