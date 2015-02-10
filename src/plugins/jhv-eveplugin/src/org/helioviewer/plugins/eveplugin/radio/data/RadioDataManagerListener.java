package org.helioviewer.plugins.eveplugin.radio.data;

import java.awt.Rectangle;
import java.util.Date;
import java.util.List;

import org.helioviewer.base.math.Interval;

public interface RadioDataManagerListener {

    public abstract void downloadRequestAnswered(Interval<Date> timeInterval, long ID, String identifier);

    public abstract void newDataAvailable(DownloadRequestData downloadRequestData, long ID);

    public abstract void downloadFinished(long ID);

    public abstract void dataNotChanged(Interval<Date> timeInterval, FrequencyInterval freqInterval, Rectangle area, List<Long> IDList, String identifier, long imageID);

    public abstract void newGlobalFrequencyInterval(FrequencyInterval interval);

    public abstract void newDataReceived(byte[] data, Interval<Date> timeInterval, FrequencyInterval freqInterval, Rectangle area, List<Long> ID, String identifier, Long imageID);

    public abstract void clearAllSavedImages(String plotIdentifier);

    public abstract void downloadRequestDataRemoved(DownloadRequestData drd, long ID);

    public abstract void downloadRequestDataVisibilityChanged(DownloadRequestData drd, long ID);

    public abstract void newDataForIDReceived(int[] data, Interval<Date> timeInterval, FrequencyInterval freqInterval, Rectangle area, Long downloadID, String identifier, Long imageID);

    public abstract void clearAllSavedImagesForID(Long downloadID, Long imageID, String plotIdentifier);

    public abstract void intervalTooBig(long iD, String identifier);

    public abstract void noDataInterval(List<Interval<Date>> noDataList, Long downloadID, String plotIdentifier);

    /**
     * The maximum frequency interval for the plot with the given plot
     * identifier was changed.
     * 
     * @param plotIdentifier
     *            The plot identifier for which the frequency interval was
     *            changed
     * @param maxFrequencyInterval
     *            The new maximum frequency interval
     */
    public abstract void frequencyIntervalUpdated(String plotIdentifier, FrequencyInterval maxFrequencyInterval);

    public abstract void newDataForIDReceived(byte[] byteData, Interval<Date> visibleImageTimeInterval, FrequencyInterval visibleImageFreqInterval, Rectangle dataSize, long downloadID, String plotIdentifier, long imageID);
}
