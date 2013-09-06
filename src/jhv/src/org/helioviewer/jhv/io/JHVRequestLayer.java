package org.helioviewer.jhv.io;

/**
 * Struct for a single image layer within a JHV request from the command line
 * 
 * @author Andre Dau
 * 
 */
public class JHVRequestLayer {
    public static final int numFields = 6;

    public static final int OBSERVATORY_INDEX = 0;
    public static final int INSTRUMENT_INDEX = 1;
    public static final int DETECTOR_INDEX = 2;
    public static final int MEASUREMENT_INDEX = 3;
    public static final int VISIBILITY_INDEX = 4;
    public static final int OPACITY_INDEX = 5;

    public String observatory;
    public String instrument;
    public String detector;
    public String measurement;
    public boolean visibility;
    public int opacity;
}
