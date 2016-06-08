package org.helioviewer.jhv.plugins.pfssplugin;

// Important settings
public class PfssSettings {

    // Needed for the JHV plugin initialization
    public static final String PLUGIN_LOCATION = "PfssPlugin";

    // Maximal preload of cache data
    public static final int CACHE_SIZE = 365 * 4;

    public static final float LINE_ALPHA = 1;

    public static final float LINE_WIDTH = 2;

    public static final int POINTS_PER_LINE = 40;

    public static final String baseURL = "http://swhv.oma.be/magtest/pfss/";

    public static int qualityReduction = 8;

    public static boolean fixedColor = false;

}
