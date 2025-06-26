package org.helioviewer.jhv;

public class DisplaySettings {

    public enum TimeMode {Observer, Sun, Earth}

    public enum UITheme {Dark, Light}

    private static boolean normalizeAIA;
    private static boolean normalizeRadius;
    private static TimeMode timeMode;
    private static UITheme uiTheme;

    static {
        normalizeAIA = Boolean.parseBoolean(Settings.getProperty("display.normalizeAIA"));
        normalizeRadius = Boolean.parseBoolean(Settings.getProperty("display.normalizeRadius"));

        TimeMode setTimeMode = TimeMode.Observer;
        try {
            setTimeMode = TimeMode.valueOf(Settings.getProperty("display.time"));
        } catch (Exception ignore) {
        }
        timeMode = setTimeMode;

        UITheme setUITheme = UITheme.Dark;
        try {
            setUITheme = UITheme.valueOf(Settings.getProperty("display.theme"));
        } catch (Exception ignore) {
        }
        uiTheme = setUITheme;
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

    public static void enableUITheme() {
        switch (uiTheme) {
            case Dark -> com.formdev.flatlaf.intellijthemes.FlatDarkFlatIJTheme.setup();
            case Light -> com.formdev.flatlaf.intellijthemes.FlatLightFlatIJTheme.setup();
        }
    }

    public static UITheme getUITheme() {
        return uiTheme;
    }

    public static void setUITheme(UITheme theme) {
        Settings.setProperty("display.theme", theme.toString());
        uiTheme = theme;
    }

}
