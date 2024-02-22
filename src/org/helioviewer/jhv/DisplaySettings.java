package org.helioviewer.jhv;

import org.helioviewer.jhv.time.TimeMode;

public class DisplaySettings {

    private static boolean normalizeAIA;
    private static boolean normalizeRadius;
    private static TimeMode timeMode;

    static {
        setup();
    }

    public static void setup() {
        normalizeAIA = Boolean.parseBoolean(Settings.getProperty("display.normalizeAIA"));
        normalizeRadius = Boolean.parseBoolean(Settings.getProperty("display.normalizeRadius"));

        TimeMode setTimeMode = TimeMode.Observer;
        try {
            setTimeMode = TimeMode.valueOf(Settings.getProperty("display.time"));
        } catch (Exception ignore) {
        }
        timeMode = setTimeMode;
    }

    public static boolean normalizeAIA() {
        return normalizeAIA;
    }

    public static boolean normalizeRadius() {
        return normalizeRadius;
    }

    public static TimeMode timeMode() {
        return timeMode;
    }

}
