package org.helioviewer.gl3d.plugin.pfss.settings;

import org.helioviewer.gl3d.scenegraph.math.GL3DVec3f;

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
	public final static int PRELOAD = 5;

	/**
	 * Maximal preload of cache data.
	 */
	public final static int CACHE_SIZE = 25;

	/**
	 * Stepsize for the Loading data
	 */
	public final static int LOD_STEPS = 1;

	/**
	 * URL of the dataserver
	 */
	public final static String INFOFILE_URL = "http://soleil.i4ds.ch/sol-win/";

	/**
	 * Color of the line (from sunradius to outside)
	 */
	public final static GL3DVec3f SUN_OUT_LINE_COLOR = new GL3DVec3f(0f, 1f, 0f);

	/**
	 * Color of the line (from outside to sunradius)
	 */
	public final static GL3DVec3f OUT_SUN_LINE_COLOR = new GL3DVec3f(1f, 0f, 1f);

	/**
	 * Color of the line (from sunradius to sunradius)
	 */
	public final static GL3DVec3f SUN_SUN_LINE_COLOR = new GL3DVec3f(1f, 1f, 1f);

	/**
	 * Alpha-value of lines
	 */
	public final static float LINE_ALPHA = 1.0f;

	/**
	 * Cos of angle for LOD in degree or radian, if you would use degree
	 * Math.toRadian(DEGREEVALUE))
	 */
	public final static double ANGLE_OF_LOD = Math.cos(Math.toRadians(5.0));

	/**
	 * Linewidth for the OpenGL visualization
	 */
	public final static float LINE_WIDTH = 1.5f;
}
