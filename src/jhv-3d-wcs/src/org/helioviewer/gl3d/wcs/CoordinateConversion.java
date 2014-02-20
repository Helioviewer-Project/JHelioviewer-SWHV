package org.helioviewer.gl3d.wcs;

/**
 * The {@link CoordinateConversion} provided a way to convert a
 * {@link CoordinateVector} from a source {@link CoordinateSystem} to a target
 * system.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public interface CoordinateConversion {
    public CoordinateSystem getSourceCoordinateSystem();

    public CoordinateSystem getTargetCoordinateSystem();

    public CoordinateVector convert(CoordinateVector vector);
}
