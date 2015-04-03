package org.helioviewer.jhv.plugins.pfssplugin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
     * Maximal preload of cache data.
     */
    public final static int PRELOAD = 25;

    /**
     * Maximal preload of cache data.
     */
    public final static int CACHE_SIZE = 125;

    /**
     * Alpha-value of lines
     */
    public final static float LINE_ALPHA = 1.0f;

    /**
     * Linewidth for the OpenGL visualization
     */
    public final static float LINE_WIDTH = 0.6f;

    public final static int POINTS_PER_LINE = 40;

    public static String baseUrl = "http://swhv.oma.be/magtest/pfss/";

    public static int qualityReduction = 8;

    public static boolean fixedColor = false;
    public final static ExecutorService pfssPool = Executors.newFixedThreadPool(5);
    public final static ExecutorService pfssNewLoadPool = Executors.newFixedThreadPool(1);

}
