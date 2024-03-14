package org.helioviewer.jhv;

import org.helioviewer.jhv.time.TimeMode;

public class DisplaySettings {

    private static boolean normalizeAIA;
    private static boolean normalizeRadius;
    private static TimeMode timeMode;

    static {
        normalizeAIA = Boolean.parseBoolean(Settings.getProperty("display.normalizeAIA"));
        normalizeRadius = Boolean.parseBoolean(Settings.getProperty("display.normalizeRadius"));

        TimeMode setTimeMode = TimeMode.Observer;
        try {
            setTimeMode = TimeMode.valueOf(Settings.getProperty("display.time"));
        } catch (Exception ignore) {
        }
        timeMode = setTimeMode;
    }

    public static boolean getNormalizeAIA() {
        return normalizeAIA;
    }

    public static void setNormalizeAIA(boolean b) {
        Settings.setProperty("display.normalizeAIA", Boolean.toString(b));
        normalizeAIA = b;
    }

    public static boolean getNormalizeRadius() {
        return normalizeRadius;
    }


    public static void setNormalizeRadius(boolean b) {
        Settings.setProperty("display.normalizeRadius", Boolean.toString(b));
        normalizeRadius = b;
    }

    public static TimeMode getTimeMode() {
        return timeMode;
    }

    public static void setTimeMode(TimeMode mode) {
        Settings.setProperty("display.time", mode.toString());
        timeMode = mode;
    }

}
