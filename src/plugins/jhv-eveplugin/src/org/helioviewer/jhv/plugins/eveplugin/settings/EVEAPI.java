package org.helioviewer.jhv.plugins.eveplugin.settings;

/**
 *
 * @author Stephan Pagel
 * */
public class EVEAPI {
    /** Date format the EVE API is using. */
    public final static String API_DATE_FORMAT = "yyyy-MM-dd";

    /** URL of the EVE API */
    public final static String API_URL = "http://lasp.colorado.edu/eve/data_access/service/retrieve_data/cgi-bin/retrieve_l2_averages.cgi?";

    public final static String API_URL_PARAMETER_STARTDATE = "start_date=";

    public final static String API_URL_PARAMETER_ENDDATE = "end_date=";

    public final static String API_URL_PARAMETER_TYPE = "timeline=";

    public final static String API_URL_PARAMETER_FORMAT = "data_format=";

    /**
     * Enumeration specifies possible values for
     * {@link API_URL_PARAMETER_FORMAT}.
     */
    public enum API_URL_PARAMETER_FORMAT_VALUES {
        JSON("json");

        private final String text;

        private API_URL_PARAMETER_FORMAT_VALUES(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

}
