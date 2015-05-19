package org.helioviewer.jhv.plugins.eveplugin.settings;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * 
 * @author Stephan Pagel
 * */
public class EVEAPI {
    /** Date format the EVE API is using. */
    public final static String API_DATE_FORMAT = "yyyy-MM-dd";

    /** URL of the EVE API */
    public final static String API_URL = "http://lasp.colorado.edu/eve/data_access/service/retrieve_data/cgi-bin/retrieve_l2_averages.cgi?";

    /**  */
    public final static String API_URL_PARAMETER_STARTDATE = "start_date=";

    /**  */
    public final static String API_URL_PARAMETER_ENDDATE = "end_date=";

    /**  */
    public final static String API_URL_PARAMETER_TYPE = "timeline=";
    /**  */
    public final static String API_URL_PARAMETER_FORMAT = "data_format=";

    private final static String GROUP_AIA = "Bands Data (AIA)";
    private final static String GROUP_BANDS = "Bands Data";
    private final static String GROUP_DIODES = "Diodes Data";
    private final static String GROUP_LINES = "Lines Data";

    private final static String UNIT_AIA = "Average counts / AIA pixel / second";
    private final static String UNIT_ALL = "Microwatts / meter^2";

    /**
     * Enumeration specifies possible values for {@link API_URL_PARAMETER_TYPE}.
     */
    public enum API_URL_PARAMETER_TYPE_VALUES {
        BANDSDATA_AIA_A94(GROUP_AIA, "AIA_A94", UNIT_AIA), BANDSDATA_AIA_A131(GROUP_AIA, "AIA_A131", UNIT_AIA), BANDSDATA_AIA_A171(GROUP_AIA, "AIA_A171", UNIT_AIA), BANDSDATA_AIA_A193(GROUP_AIA, "AIA_A193", UNIT_AIA), BANDSDATA_AIA_A211(GROUP_AIA, "AIA_A211", UNIT_AIA), BANDSDATA_AIA_A304(GROUP_AIA, "AIA_A304", UNIT_AIA), BANDSDATA_AIA_A335(GROUP_AIA, "AIA_A335", UNIT_AIA),

        BANDSDATA_E37_45(GROUP_BANDS, "E37-45", UNIT_ALL), BANDSDATA_E7_37(GROUP_BANDS, "E7-37", UNIT_ALL), BANDSDATA_GOES_14_EUV_A(GROUP_BANDS, "GOES-14_EUV-A", UNIT_ALL), BANDSDATA_GOES_14_EUV_B(GROUP_BANDS, "GOES-14_EUV-B", UNIT_ALL), BANDSDATA_MA171(GROUP_BANDS, "MA171", UNIT_ALL), BANDSDATA_MA257(GROUP_BANDS, "MA257", UNIT_ALL), BANDSDATA_MA304(GROUP_BANDS, "MA304", UNIT_ALL), BANDSDATA_MA366(GROUP_BANDS, "MA366", UNIT_ALL), BANDSDATA_MEGS_A1(GROUP_BANDS, "MEGS-A1", UNIT_ALL), BANDSDATA_MEGS_A2(GROUP_BANDS, "MEGS-A2", UNIT_ALL), BANDSDATA_MEGS_B_both(GROUP_BANDS, "MEGS-B_both", UNIT_ALL), BANDSDATA_MEGS_B_short(GROUP_BANDS, "MEGS-B_short", UNIT_ALL),

        DIODSDATA_ESP171(GROUP_DIODES, "ESP171", UNIT_ALL), DIODSDATA_ESP257(GROUP_DIODES, "ESP257", UNIT_ALL), DIODSDATA_ESP304(GROUP_DIODES, "ESP304", UNIT_ALL), DIODSDATA_ESP366(GROUP_DIODES, "ESP366", UNIT_ALL), DIODSDATA_ESPQ(GROUP_DIODES, "ESPQ", UNIT_ALL), DIODSDATA_MEGSP1216(GROUP_DIODES, "MEGSP1216", UNIT_ALL),

        LINESDATA_Fe_XVIII_94(GROUP_LINES, "Fe_XVIII_94", UNIT_ALL), LINESDATA_Fe_VIII_131(GROUP_LINES, "Fe_VIII_131", UNIT_ALL), LINESDATA_Fe_XX_133(GROUP_LINES, "Fe_XX_133", UNIT_ALL), LINESDATA_Fe_IX_171(GROUP_LINES, "Fe_IX_171", UNIT_ALL), LINESDATA_Fe_X_177(GROUP_LINES, "Fe_X_177", UNIT_ALL), LINESDATA_Fe_XI_180(GROUP_LINES, "Fe_XI_180", UNIT_ALL), LINESDATA_Fe_XII_195(GROUP_LINES, "Fe_XII_195", UNIT_ALL), LINESDATA_Fe_XIII_202(GROUP_LINES, "Fe_XIII_202", UNIT_ALL), LINESDATA_Fe_XIV_211(GROUP_LINES, "Fe_XIV_211", UNIT_ALL), LINESDATA_He_II_256(GROUP_LINES, "He_II_256", UNIT_ALL), LINESDATA_Fe_XV_284(GROUP_LINES, "Fe_XV_284", UNIT_ALL), LINESDATA_He_II_304(GROUP_LINES, "He_II_304", UNIT_ALL), LINESDATA_Fe_XVI_335(GROUP_LINES, "Fe_XVI_335", UNIT_ALL), LINESDATA_Fe_XVI_361(GROUP_LINES, "Fe_XVI_361", UNIT_ALL), LINESDATA_Mg_IX_368(GROUP_LINES, "Mg_IX_368", UNIT_ALL), LINESDATA_Ne_VII_465(GROUP_LINES, "Ne_VII_465", UNIT_ALL), LINESDATA_Si_XII_499(GROUP_LINES, "Si_XII_499", UNIT_ALL), LINESDATA_O_III_526(GROUP_LINES, "O_III_526", UNIT_ALL), LINESDATA_O_IV_554(GROUP_LINES, "O_IV_554", UNIT_ALL), LINESDATA_He_I_584(GROUP_LINES, "He_I_584", UNIT_ALL), LINESDATA_O_III_600(GROUP_LINES, "O_III_600", UNIT_ALL), LINESDATA_Mg_X_625(GROUP_LINES, "Mg_X_625", UNIT_ALL), LINESDATA_O_V_630(GROUP_LINES, "O_V_630", UNIT_ALL);

        private final String group;
        private final String name;
        private final String unit;

        private API_URL_PARAMETER_TYPE_VALUES(String group, String name, String unit) {
            this.group = group;
            this.name = name;
            this.unit = unit;
        }

        public String getUnit() {
            return unit;
        }

        public final String getGroup() {
            return group;
        }

        public static final String[] getGroups() {
            HashSet<String> set = new HashSet<String>();

            for (API_URL_PARAMETER_TYPE_VALUES v : API_URL_PARAMETER_TYPE_VALUES.values()) {
                set.add(v.getGroup());
            }

            String[] groups = set.toArray(new String[0]);
            Arrays.sort(groups);

            return groups;
        }

        public final String getName() {
            return name;
        }

        public static final String[] getNames() {
            HashSet<String> set = new HashSet<String>();

            for (API_URL_PARAMETER_TYPE_VALUES v : API_URL_PARAMETER_TYPE_VALUES.values()) {
                set.add(v.getName());
            }

            return set.toArray(new String[0]);
        }

        public static final String[] getNames(final String group) {
            HashSet<String> set = new HashSet<String>();

            for (API_URL_PARAMETER_TYPE_VALUES v : API_URL_PARAMETER_TYPE_VALUES.values()) {
                if (v.getGroup().equals(group))
                    set.add(v.getName());
            }

            return set.toArray(new String[0]);
        }

        public static final API_URL_PARAMETER_TYPE_VALUES[] getValues(final String group) {
            LinkedList<API_URL_PARAMETER_TYPE_VALUES> list = new LinkedList<API_URL_PARAMETER_TYPE_VALUES>();

            for (API_URL_PARAMETER_TYPE_VALUES v : API_URL_PARAMETER_TYPE_VALUES.values()) {
                if (v.getGroup().equals(group))
                    list.add(v);
            }

            return list.toArray(new API_URL_PARAMETER_TYPE_VALUES[0]);
        }

        @Override
        public String toString() {
            return name;
        }
    };

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

    public enum API_RESOLUTION_AVERAGES {
        MINUTE_1("1 minute", 60000);
        // TODO SP: Actually the web service with lasp.colorado.edu URL does not
        // support other
        // resolutions as the 1 Minute. It would be nice if it would support the
        // following
        // ones.
        // MINUTE_5 ("5 minute", 300000),
        // MINUTE_15 ("15 minute", 900000),
        // HOUR_1 ("1 hour", 3600000),
        // DAY_1 ("Daily", 86400000);

        private final String text;
        private final long milliseconds;

        private API_RESOLUTION_AVERAGES(String text, long milliseconds) {
            this.text = text;
            this.milliseconds = milliseconds;
        }

        public long getMilliSeconds() {
            return milliseconds;
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
