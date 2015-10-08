package org.helioviewer.jhv.plugins.pfssplugin;

/**
 * Important settings
 *
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssSettings {

    /**
     * Needed for the JHV plugin initialization
     */
    public final static String PLUGIN_LOCATION = "PfssPlugin";

    /**
     * Maximal preload of cache data
     */
    public final static int CACHE_SIZE = 365 * 4;

    public final static float LINE_ALPHA = 1;

    public final static double LINE_WIDTH = 0.5;

    public final static int POINTS_PER_LINE = 40;

    public static String baseUrl = "http://swhv.oma.be/magtest/pfss/";

    public static int qualityReduction = 8;

    public static boolean fixedColor = false;

}
