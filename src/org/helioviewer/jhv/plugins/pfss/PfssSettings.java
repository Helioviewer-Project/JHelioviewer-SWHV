package org.helioviewer.jhv.plugins.pfss;

public class PfssSettings {

    // Maximal preload of cache data
    public static final int CACHE_SIZE = 365 * 4;

    public static final float LINE_ALPHA = 1;
    public static final float LINE_WIDTH = 2;
    public static final int POINTS_PER_LINE = 40;

    public static final String baseURL = "http://swhv.oma.be/magtest/pfss/";
    public static final String availabilityURL = "http://swhv.oma.be/availability/pfss/availability/availability.html";

    public static int qualityReduction = 8;
    public static boolean fixedColor = false;

}
