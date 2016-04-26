package org.helioviewer.jhv.plugins.eveplugin.settings;

public class EVESettings {

    public static final int MAX_WORKER_THREADS = 12;

    /**
     * Maximum number of days per download request. If an interval is bigger
     * than the given number of days, the interval will be split up into sub
     * intervals. For each sub interval a single request will be send.
     * */
    public static final int DOWNLOADER_MAX_DAYS_PER_BLOCK = 21;

    public static final String RADIO_OBSERVATION_UI_NAME = "Radio data";

    public static final String OBSERVATION_UI_NAME = "1D time series";

}
