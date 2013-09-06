package org.helioviewer.plugins.eveplugin.controller;

import java.util.Date;

import org.helioviewer.base.math.Interval;

/**
 * @author Stephan Pagel
 * */
public interface DownloadControllerListener {

    public void downloadStarted(final Band band, final Interval<Date> interval);
    public void downloadFinished(final Band band, final Interval<Date> interval, final int activeBandDownloads);
}
