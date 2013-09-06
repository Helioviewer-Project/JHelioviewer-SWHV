package org.helioviewer.jhv.io;

/**
 * Struct for a JHV request from the command line
 * 
 * @author Andre Dau
 * 
 */
public class JHVRequest {
    public String startTime = null;
    public String endTime = null;
    public double imageScale = -1;
    public JHVRequestLayer[] imageLayers = null;
    public String cadence = null;
    public boolean linked = false;
}
