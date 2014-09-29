package org.helioviewer.plugins.eveplugin.radio.data;

import java.util.Date;
import java.util.List;

import org.helioviewer.base.math.Interval;

public interface RadioDownloaderListener {

    public abstract void intervalTooBig(Date requestedStartTime, Date requestedEndTime, long ID, String identifier);

    public abstract void newJPXFilesDownloaded(List<DownloadedJPXData> jpxFiles, Date requestedStartTime, Date requestedEndTime, Long downloadID, String identifier);

    public abstract void newAdditionalDataDownloaded(List<DownloadedJPXData> jpxFiles, Long downloadID, String plotIdentifier, double ratioX, double ratioY);

    public abstract void newNoData(List<Interval<Date>> noDataList, String identifier, long downloadID);

    /**
     * Instructs the radio downloader listener to remove all the spectrograms
     * for the plot identifier by the identifier.
     * 
     * @param identifier
     *            The identifier of the plot from which the spectrograms should
     *            be removed
     */
    public abstract void removeSpectrograms(String identifier);
}
