package org.helioviewer.jhv.plugins.swek.sources.comesep;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
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

public class ComesepDownloader implements SWEKDownloader {

    /** The hek source properties */
    private final Properties comesepSourceProperties;

    /**
     * Default constructor.
     */
    public ComesepDownloader() {
        ComesepProperties csp = ComesepProperties.getSingletonInstance();
        comesepSourceProperties = csp.getComesepProperties();
    }

    @Override
    public void stopDownload() {
        // TODO Auto-generated method stub

    }

    @Override
    public InputStream downloadData(SWEKEventType eventType, Date startDate, Date endDate, List<SWEKParam> params, int page) {
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
     * @param page
     *            the page that should be downloaded
     * @return the url represented as string
     */
    private String createURL(SWEKEventType eventType, Date startDate, Date endDate, List<SWEKParam> params) {
        StringBuilder baseURL = new StringBuilder(comesepSourceProperties.getProperty("comesepsource.baseurl")).append("?");
        baseURL = appendModel(baseURL, params).append("&");
        baseURL = appendEventStartTime(baseURL, startDate).append("&");
        baseURL = appendEventEndTime(baseURL, endDate).append("&");
        return baseURL.toString();
    }

    /**
     * Appends the event end time to the given URL.
     * 
     * @param baseURL
     *            the current URL
     * @param endDate
     *            the end date
     * @return the current URL extended with the end time
     */
    private StringBuilder appendEventEndTime(StringBuilder baseURL, Date endDate) {
        return baseURL.append("enddate=").append(formatDate(endDate));
    }

    /**
     * Appends the event start time to the given URL.
     * 
     * @param baseURL
     *            the current URL
     * @param startDate
     *            the start date
     * @return the current URL extended with the start time
     */
    private StringBuilder appendEventStartTime(StringBuilder baseURL, Date startDate) {
        return baseURL.append("startdate=").append(formatDate(startDate));
    }

    /**
     * Extracts the model from the list of parameters and appends the model to
     * the given URL.
     * 
     * @param baseURL
     *            the current URL
     * @param params
     *            the list with params
     * @return
     */
    private StringBuilder appendModel(StringBuilder baseURL, List<SWEKParam> params) {
        String model = "";
        for (SWEKParam p : params) {
            if (p.param.equals("provider")) {
                model = p.value;
                break;
            }
        }
        return baseURL.append("model=").append(model);
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
