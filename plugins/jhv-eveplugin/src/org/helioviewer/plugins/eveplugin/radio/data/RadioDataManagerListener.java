package org.helioviewer.plugins.eveplugin.radio.data;

import java.awt.Rectangle;
import java.util.Date;
import java.util.List;

import org.helioviewer.base.math.Interval;

public interface RadioDataManagerListener {
	public abstract void downloadRequestAnswered(FrequencyInterval freqInterval, Interval<Date> timeInterval,long ID, String identifier);
	public abstract void newDataAvailable(DownloadRequestData downloadRequestData, long ID);
	public abstract void downloadFinished(long ID);
	public abstract void dataNotChanged(); 
	public abstract void newGlobalFrequencyInterval(FrequencyInterval interval);
	public abstract void newDataReceived(byte[] data, Interval<Date> timeInterval, FrequencyInterval freqInterval, Rectangle area, List<Long> ID, String identifier);
	public abstract void clearAllSavedImages();
	public abstract void downloadRequestDataRemoved(DownloadRequestData drd, long ID);
	public abstract void downloadRequestDataVisibilityChanged(DownloadRequestData drd, long ID);
}
