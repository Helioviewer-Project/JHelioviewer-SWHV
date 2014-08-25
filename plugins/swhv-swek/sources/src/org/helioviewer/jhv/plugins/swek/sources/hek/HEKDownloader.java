package org.helioviewer.jhv.plugins.swek.sources.hek;

import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.sources.SWEKDownloader;

public class HEKDownloader implements SWEKDownloader {

    /** The hek source properties */
    private final Properties hekSourceProperties;

    /**
     * Default constructor.
     */
    public HEKDownloader() {
        HEKSourceProperties hsp = HEKSourceProperties.getSingletonInstance();
        this.hekSourceProperties = hsp.getHEKSourceProperties();
    }

    @Override
    public void stopDownload() {
        // TODO Auto-generated method stub

    }

    @Override
    public InputStream downloadData(SWEKEventType eventType, Date startDate, Date endDate) {
        createURL(eventType, startDate, endDate);

        return null;
    }

    private String createURL(SWEKEventType eventType, Date startDate, Date endDate) {
        StringBuilder baseURL = new StringBuilder(this.hekSourceProperties.getProperty("heksource.baseurl")).append("?");
        baseURL = appendCmd(baseURL, eventType, startDate, endDate).append("&");
        baseURL = appendType(baseURL, eventType, startDate, endDate).append("&");
        baseURL = appendEventType(baseURL, eventType, startDate, endDate).append("&");
        baseURL = appendEventCoorSys(baseURL, eventType, startDate, endDate).append("&");
        baseURL = appendX1X2Y1Y2(baseURL, eventType, startDate, endDate).append("&");
        baseURL = appendCosec(baseURL, eventType, startDate, endDate).append("&");
        baseURL = appendParams(baseURL, eventType, startDate, endDate).append("&");
        return baseURL.toString();
    }

    private StringBuilder appendCmd(StringBuilder baseURL, SWEKEventType eventType, Date startDate, Date endDate) {
        return baseURL.append("cmd=search");
    }

    private StringBuilder appendType(StringBuilder baseURL, SWEKEventType eventType, Date startDate, Date endDate) {
        return baseURL.append("type=column");
    }

    private StringBuilder appendEventType(StringBuilder baseURL, SWEKEventType eventType, Date startDate, Date endDate) {
        return baseURL.append("event_type=").append(HEKEventFactory.getHEKEvent(eventType.getEventName()));
    }

    private StringBuilder appendEventCoorSys(StringBuilder baseURL, SWEKEventType eventType, Date startDate, Date endDate) {
        return baseURL.append("event_coordsys=helioprojective");
    }

    private StringBuilder appendX1X2Y1Y2(StringBuilder baseURL, SWEKEventType eventType, Date startDate, Date endDate) {
        // TODO Auto-generated method stub
        return null;
    }

    private StringBuilder appendCosec(StringBuilder baseURL, SWEKEventType eventType, Date startDate, Date endDate) {
        // TODO Auto-generated method stub
        return null;
    }

    private StringBuilder appendParams(StringBuilder baseURL, SWEKEventType eventType, Date startDate, Date endDate) {
        // TODO Auto-generated method stub
        return null;
    }

}
