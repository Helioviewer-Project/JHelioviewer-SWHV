package org.helioviewer.jhv.plugins.eveplugin.radio.data;

import java.awt.Rectangle;
import java.util.Date;
import java.util.List;

import org.helioviewer.jhv.base.interval.Interval;

public interface RadioDataManagerListener {

    public abstract void downloadRequestAnswered(Interval<Date> timeInterval);

    public abstract void newDataAvailable(DownloadRequestData downloadRequestData);

    public abstract void downloadRequestDataRemoved(DownloadRequestData drd);

    public abstract void downloadRequestDataVisibilityChanged(DownloadRequestData drd);

    public abstract void newDataForIDReceived(int[] data, Interval<Date> timeInterval, FrequencyInterval visibleFreqInterval, FrequencyInterval imageFreqInterval, Rectangle area, long imageID);

    public abstract void clearAllSavedImagesForID(long imageID);

    public abstract void intervalTooBig();

    public abstract void noDataInterval(List<Interval<Date>> noDataList);

    public abstract void newDataForIDReceived(byte[] byteData, Interval<Date> visibleImageTimeInterval, FrequencyInterval visibleImageFreqInterval, FrequencyInterval imageFreqInterval, Rectangle dataSize, long imageID);

}
