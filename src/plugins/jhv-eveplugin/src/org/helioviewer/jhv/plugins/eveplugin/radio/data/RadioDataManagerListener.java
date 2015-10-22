package org.helioviewer.jhv.plugins.eveplugin.radio.data;

import java.awt.Rectangle;
import java.util.Date;
import java.util.List;

import org.helioviewer.jhv.base.interval.Interval;

public interface RadioDataManagerListener {

    public abstract void downloadRequestAnswered(Interval<Date> timeInterval, long ID);

    public abstract void newDataAvailable(DownloadRequestData downloadRequestData, long ID);

    public abstract void downloadFinished(long ID);

    public abstract void dataNotChanged(Interval<Date> timeInterval, FrequencyInterval freqInterval, Rectangle area, List<Long> IDList, long imageID);

    public abstract void newGlobalFrequencyInterval(FrequencyInterval interval);

    public abstract void newDataReceived(byte[] data, Interval<Date> timeInterval, FrequencyInterval freqInterval, Rectangle area, List<Long> ID, Long imageID);

    public abstract void clearAllSavedImages();

    public abstract void downloadRequestDataRemoved(DownloadRequestData drd, long ID);

    public abstract void downloadRequestDataVisibilityChanged(DownloadRequestData drd, long ID);

    public abstract void newDataForIDReceived(int[] data, Interval<Date> timeInterval, FrequencyInterval freqInterval, Rectangle area, Long downloadID, Long imageID);

    public abstract void clearAllSavedImagesForID(Long downloadID, Long imageID);

    public abstract void intervalTooBig(long iD);

    public abstract void noDataInterval(List<Interval<Date>> noDataList, Long downloadID);

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
    public abstract void frequencyIntervalUpdated(FrequencyInterval maxFrequencyInterval);

    public abstract void newDataForIDReceived(byte[] byteData, Interval<Date> visibleImageTimeInterval, FrequencyInterval visibleImageFreqInterval, Rectangle dataSize, long downloadID, long imageID);
}
