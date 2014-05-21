package org.helioviewer.plugins.eveplugin.radio.data;

import java.util.Date;
import java.util.List;

import org.helioviewer.base.math.Interval;
import org.helioviewer.viewmodel.view.ImageInfoView;

public interface RadioDownloaderListener {
    public abstract void newImageViewDownloaded(ImageInfoView v, Date requestedStartTime, Date requestedEndTime, long ID, String identifier);

    public abstract void intervalTooBig(Date requestedStartTime, Date requestedEndTime, long ID, String identifier);

    public abstract void newJPXFilesDownloaded(List<DownloadedJPXData> jpxFiles, Date requestedStartTime, Date requestedEndTime, Long downloadID, String identifier);

    public abstract void newAdditionalDataDownloaded(List<DownloadedJPXData> jpxFiles, Long downloadID, String plotIdentifier, double ratioX, double ratioY);

    public abstract void newNoData(List<Interval<Date>> noDataList, String identifier, long downloadID);
}
