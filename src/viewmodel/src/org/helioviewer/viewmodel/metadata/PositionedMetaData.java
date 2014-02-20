package org.helioviewer.viewmodel.metadata;

/**
 * Meta Data providing information about observer position.
 * 
 * Position data is required when the observer is significantly off the
 * earth-sun line, such as STEREO observations.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public interface PositionedMetaData {

    public boolean isHEEQProvided();

    public double getHEEQX();

    public double getHEEQY();

    public double getHEEQZ();

}
