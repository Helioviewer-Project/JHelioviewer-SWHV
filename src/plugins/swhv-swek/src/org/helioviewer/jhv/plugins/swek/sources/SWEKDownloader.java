package org.helioviewer.jhv.plugins.swek.sources;

import java.util.Date;
import java.util.List;

import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.plugins.swek.download.SWEKParam;

public interface SWEKDownloader {

    public boolean extern2db(JHVEventType eventType, Date startDate, Date endDate, List<SWEKParam> params);

}
