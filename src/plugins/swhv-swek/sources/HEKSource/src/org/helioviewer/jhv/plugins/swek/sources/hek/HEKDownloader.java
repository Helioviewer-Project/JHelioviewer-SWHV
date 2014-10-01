package org.helioviewer.jhv.plugins.swek.sources.hek;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.helioviewer.base.DownloadStream;
import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.download.SWEKParam;
import org.helioviewer.jhv.plugins.swek.sources.SWEKDownloader;

public class HEKDownloader implements SWEKDownloader {

    /** The hek source properties */
    private final Properties hekSourceProperties;

    /**
     * Default constructor.
     */
    public HEKDownloader() {
        HEKSourceProperties hsp = HEKSourceProperties.getSingletonInstance();
        hekSourceProperties = hsp.getHEKSourceProperties();
    }

    @Override
    public void stopDownload() {
        // TODO Auto-generated method stub

    }

    @Override
    public InputStream downloadData(SWEKEventType eventType, Date startDate, Date endDate, List<SWEKParam> params) {
        String urlString = createURL(eventType, startDate, endDate, params);
        Log.info("Download events using following URL: " + urlString);
        try {
            DownloadStream ds = new DownloadStream(new URL(urlString), JHVGlobals.getStdConnectTimeout(), JHVGlobals.getStdReadTimeout());
            return ds.getInput();
        } catch (MalformedURLException e) {
            Log.error("Could not create URL from given string: " + urlString + " error : " + e);
            return null;
        } catch (IOException e) {
            Log.error("Could not create input stream for given URL: " + urlString + " error : " + e);
            return null;
        }

    }

    /**
     * Creates the download URL for the HEK.
     * 
     * @param eventType
     *            the event type that should be downloaded
     * @param startDate
     *            the start date of the interval over which to download the
     *            event
     * @param endDate
     *            the end date of the interval over which to download the event
     * @return the url represented as string
     */
    private String createURL(SWEKEventType eventType, Date startDate, Date endDate, List<SWEKParam> params) {
        StringBuilder baseURL = new StringBuilder(hekSourceProperties.getProperty("heksource.baseurl")).append("?");
        baseURL = appendCmd(baseURL, eventType, startDate, endDate).append("&");
        baseURL = appendType(baseURL, eventType, startDate, endDate).append("&");
        baseURL = appendEventType(baseURL, eventType, startDate, endDate).append("&");
        baseURL = appendEventCoorSys(baseURL, eventType, startDate, endDate).append("&");
        baseURL = appendX1X2Y1Y2(baseURL, eventType, startDate, endDate).append("&");
        baseURL = appendCosec(baseURL, eventType, startDate, endDate).append("&");
        baseURL = appendParams(baseURL, eventType, startDate, endDate, params).append("&");
        baseURL = appendEventStartTime(baseURL, eventType, startDate).append("&");
        baseURL = appendEventEndTime(baseURL, eventType, endDate).append("&");
        return baseURL.toString();
    }

    /**
     * Appends the command to the given URL.
     * 
     * @param baseURL
     *            the current URL
     * @param eventType
     *            the event type
     * @param startDate
     *            the start date
     * @param endDate
     *            the end date
     * @return the current URL extended with the command
     */
    private StringBuilder appendCmd(StringBuilder baseURL, SWEKEventType eventType, Date startDate, Date endDate) {
        return baseURL.append("cmd=search");
    }

    /**
     * Appends the type to the given URL.
     * 
     * @param baseURL
     *            the current URL
     * @param eventType
     *            the event type
     * @param startDate
     *            the start date
     * @param endDate
     *            the end date
     * @return the current URL extended with the type
     */
    private StringBuilder appendType(StringBuilder baseURL, SWEKEventType eventType, Date startDate, Date endDate) {
        return baseURL.append("type=column");
    }

    /**
     * Appends the event type to the given URL.
     * 
     * @param baseURL
     *            the current URL
     * @param eventType
     *            the event type
     * @param startDate
     *            the start date
     * @param endDate
     *            the end date
     * @return the current URL extended with the event type
     */
    private StringBuilder appendEventType(StringBuilder baseURL, SWEKEventType eventType, Date startDate, Date endDate) {
        return baseURL.append("event_type=").append(HEKEventFactory.getHEKEvent(eventType.getEventName()).getAbbriviation());
    }

    /**
     * Appends the event coordinate system to the given URL.
     * 
     * @param baseURL
     *            the current URL
     * @param eventType
     *            the event type
     * @param startDate
     *            the start date
     * @param endDate
     *            the end date
     * @return the current URL extended with the event coordinate system
     */
    private StringBuilder appendEventCoorSys(StringBuilder baseURL, SWEKEventType eventType, Date startDate, Date endDate) {
        return baseURL.append("event_coordsys=").append(eventType.getCoordinateSystem());
    }

    /**
     * Appends the spatial region to the given URL.
     * 
     * @param baseURL
     *            the current URL
     * @param eventType
     *            the event type
     * @param startDate
     *            the start date
     * @param endDate
     *            the end date
     * @return the current URL extended with the spatial region
     */
    private StringBuilder appendX1X2Y1Y2(StringBuilder baseURL, SWEKEventType eventType, Date startDate, Date endDate) {
        baseURL.append("x1=").append(eventType.getSpatialRegion().getX1()).append("&");
        baseURL.append("x2=").append(eventType.getSpatialRegion().getX2()).append("&");
        baseURL.append("y1=").append(eventType.getSpatialRegion().getY1()).append("&");
        baseURL.append("y2=").append(eventType.getSpatialRegion().getY2()).append("&");
        return baseURL;
    }

    /**
     * Append cosec to the given URL.
     * 
     * @param baseURL
     *            the current URL
     * @param eventType
     *            the event type
     * @param startDate
     *            the start date
     * @param endDate
     *            the end date
     * @return the current URL extended with cosec
     */
    private StringBuilder appendCosec(StringBuilder baseURL, SWEKEventType eventType, Date startDate, Date endDate) {
        return baseURL.append("cosec=2");
    }

    /**
     * Appends params to the given URL.
     * 
     * @param baseURL
     *            the current URL
     * @param eventType
     *            the event type
     * @param startDate
     *            the start date
     * @param endDate
     *            the end date
     * @param params
     * @return the current URL extended with the params
     */
    private StringBuilder appendParams(StringBuilder baseURL, SWEKEventType eventType, Date startDate, Date endDate, List<SWEKParam> params) {
        int paramCount = 0;

        for (SWEKParam param : params) {
            String encodedValue;
            try {
                encodedValue = URLEncoder.encode(param.getValue(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                encodedValue = param.getValue();
            }
            if (param.getParam().toLowerCase().equals("provider")) {
                baseURL.append("param").append(paramCount).append("=").append("frm_name").append("&").append("op").append(paramCount)
                        .append("=").append(param.getOperand().URLEncodedRepresentation()).append("&").append("value").append(paramCount)
                        .append("=").append(encodedValue).append("&");
            } else {
                baseURL.append("param").append(paramCount).append("=").append(param.getParam()).append("&").append("op").append(paramCount)
                        .append("=").append(param.getOperand().URLEncodedRepresentation()).append("&").append("value").append(paramCount)
                        .append("=").append(encodedValue).append("&");
            }
            paramCount++;
        }
        return baseURL;
    }

    /**
     * Appends the event start time to the given URL.
     * 
     * @param baseURL
     *            the current URL
     * @param eventType
     *            the event type
     * @param startDate
     *            the start date
     * @return the current URL extended with the start time
     */
    private StringBuilder appendEventStartTime(StringBuilder baseURL, SWEKEventType eventType, Date startDate) {
        return baseURL.append("event_starttime=").append(formatDate(startDate));
    }

    /**
     * Appends the event end time to the given URL.
     * 
     * @param baseURL
     *            the current URL
     * @param eventType
     *            the event type
     * @param endDate
     *            the end date
     * @return the current URL extended with the end time
     */
    private StringBuilder appendEventEndTime(StringBuilder baseURL, SWEKEventType eventType, Date endDate) {
        return baseURL.append("event_endtime=").append(formatDate(endDate));
    }

    /**
     * Formats a date in the yyyy-mm-ddThh:mm:ss format.
     * 
     * @param date
     *            the date to format
     * @return the date in format yyyy-mm-ddThh:mm-ss
     */
    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return sdf.format(date);
    }
}
