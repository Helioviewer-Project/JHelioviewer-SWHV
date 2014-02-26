package org.helioviewer.plugins.eveplugin.download;

import java.util.Date;

import org.helioviewer.base.math.Interval;
import org.helioviewer.plugins.eveplugin.controller.Band;

public interface DataDownloader {
	public abstract DownloadedData downloadData(Band band, Interval<Date> interval);
}
