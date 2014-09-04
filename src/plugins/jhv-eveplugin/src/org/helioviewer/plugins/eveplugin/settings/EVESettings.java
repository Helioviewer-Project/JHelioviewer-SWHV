package org.helioviewer.plugins.eveplugin.settings;

import org.helioviewer.jhv.JHVDirectory;

/**
 * 
 * 
 * @author Stephan Pagel
 * */
public class EVESettings {

    /**
     * Maximum concurrent download threads.
     */
    public static final int DOWNLOADER_MAX_THREADS = 3;

    /**
     * Maximum number of days per download request. If an interval is bigger
     * than the given number of days, the interval will be split up into sub
     * intervals. For each sub interval a single request will be send.
     * */
    public static final int DOWNLOADER_MAX_DAYS_PER_BLOCK = 7;

    public static final String RADIO_OBSERVATION_UI_NAME = "Radio Data";

    public static final String OBSERVATION_UI_NAME = "1-D Time Series";

    public static final String DATABASE_USERNAME = "eve";
    public static final String DATABASE_PASSWORD = "eve";

    public static final String EVE_HOME = JHVDirectory.PLUGINS.getPath() + "EVEPlugin" + System.getProperty("file.separator");
    public static final String EVE_DATA = EVE_HOME + "Data" + System.getProperty("file.separator");

    public static final Long MAXIMUM_INTERVAL_RANGE_MILLI_SEC = 2L * 60 * 60 * 24 * 1000;
    public static final Long MAXIMUM_INTERVAL_RANGE_MILLI_SEC_REQ = 3L * 60 * 60 * 24 * 1000;
}
