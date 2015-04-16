package org.helioviewer.plugins.eveplugin.download;

import java.awt.Rectangle;
import java.util.Date;

import org.helioviewer.base.math.Interval;
import org.helioviewer.plugins.eveplugin.lines.data.Band;

public interface DataDownloader {
    public abstract DownloadedData downloadData(Band band, Interval<Date> interval, Rectangle plotArea);
}
