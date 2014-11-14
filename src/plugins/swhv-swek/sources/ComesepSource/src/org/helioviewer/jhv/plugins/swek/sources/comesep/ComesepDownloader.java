package org.helioviewer.jhv.plugins.swek.sources.comesep;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.download.SWEKParam;
import org.helioviewer.jhv.plugins.swek.sources.SWEKDownloader;

public class ComesepDownloader implements SWEKDownloader {

    @Override
    public void stopDownload() {
        // TODO Auto-generated method stub

    }

    @Override
    public InputStream downloadData(SWEKEventType eventType, Date startDate, Date endDate, List<SWEKParam> params, int page) {
        // TODO Auto-generated method stub
        return null;
    }

}
