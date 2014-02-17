package org.helioviewer.jhv.plugins.hekplugin.settings;

public class HEKSettings {

    /**
     * Needed for the JHV plugin initialization
     */
    public final static String PLUGIN_LOCATION = "HEKPlugin";

    /**
     * Date format the HEK is using. This information is needed for the parser.
     */
    public final static String API_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    /**
     * Events requested per page when loading the event structure.
     */
    public final static int REQUEST_STRUCTURE_PAGESIZE = 500;

    /**
     * Maximum pages to download before canceling event structure downloads.
     */
    public final static int REQUEST_STRUCTURE_MAXPAGES = 1000000;

    /**
     * Events requested per page when downloading full events.
     */
    public final static int REQUEST_EVENTS_PAGESIZE = 100;

    /**
     * Maximum pages to download before canceling full event downloads.
     */
    public final static int REQUEST_EVENTS_MAXPAGES = 1000000;

    /**
     * Maximum concurrent download threads.
     */
    public static final int DOWNLOADER_MAX_THREADS = 3;

    /**
     * Fields to be requested when downloading the event structure.
     */
    public static final String[] DOWNLOADER_DOWNLOAD_STRUCTURE_FIELDS = { "event_type", "frm_name", "kb_archivid" };

    /**
     * Fields to be requested when downloading full events. If empty, all
     * available fields will be downloaded.
     */
    public static final String[] DOWNLOADER_DOWNLOAD_EVENTS_FIELDS = {};
    // event_type", "frm_name", "kb_archivid",
    // "event_starttime", "event_endtime", "hgs_x", "hgs_y" };

    /**
     * Instead of only displaying events that are visible EXACTLY only at one
     * point in time ('NOW'), HEK displays events in a time range lasting from
     * 'NOW' - EXPAND_DURATION until 'NOW' + EXPAND_DURATION.
     * 
     * The unit is milliseconds.
     */
    public static final int MODEL_EXPAND_DURATION = 3600000 * 3; // +- 3h

}
