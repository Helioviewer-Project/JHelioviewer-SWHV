package org.helioviewer.viewmodelplugin.filter;

/**
 * Interface to provide positioning information in the compact-panel
 * 
 * @author mnuhn
 */
public interface FilterAlignmentDetails {

    /**
     * Value used to identify/define the Opacity Plugin
     */
    final static int POSITION_OPACITY = 1;

    /**
     * Value used to identify/define the Quality Selection Plugin
     */
    final static int POSITION_QUALITY = 2;

    /**
     * Value used to identify/define the Colortables Plugin
     */
    final static int POSITION_COLORTABLES = 4;

    /**
     * Value used to identify/define the Layername Plugin
     */
    static final int POSITION_LAYERNAME = 5;

    /**
     * Value used to identify/define the Sharpen Plugin
     */
    static final int POSITION_SHARPEN = 6;

    /**
     * Value used to identify/define the Gamma Plugin
     */
    static final int POSITION_GAMMA = 7;

    /**
     * Value used to identify/define the Contrast Plugin
     */
    static final int POSITION_CONTRAST = 8;

    /**
     * Value used to identify/define the Channel Mixer Plugin
     */
    static final int POSITION_CHANNELMIXER = 9;

    /**
     * Each plugin added to the compact panel needs to specify the position it
     * should be located.
     * 
     * @return - position value
     */
    public int getDetails();

}
