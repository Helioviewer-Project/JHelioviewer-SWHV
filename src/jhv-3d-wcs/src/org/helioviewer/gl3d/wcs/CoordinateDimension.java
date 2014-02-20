package org.helioviewer.gl3d.wcs;

/**
 * A Dimension of a {@link CoordinateSystem}.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public interface CoordinateDimension {
    public String getDescription();

    public double getMinValue();

    public double getMaxValue();

    public Unit getUnit();
}
