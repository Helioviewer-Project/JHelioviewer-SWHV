package org.helioviewer.gl3d.wcs.impl;

import org.helioviewer.gl3d.wcs.AbstractCoordinateSystem;
import org.helioviewer.gl3d.wcs.CoordinateConversion;
import org.helioviewer.gl3d.wcs.CoordinateDimension;
import org.helioviewer.gl3d.wcs.CoordinateSystem;
import org.helioviewer.gl3d.wcs.GenericCoordinateDimension;
import org.helioviewer.gl3d.wcs.Unit;
import org.helioviewer.gl3d.wcs.conversion.SphericalToSolarSphereConversion;

/**
 * The {@link SphericalCoordinateSystem} is a 3d-coordinate systems that allows
 * specifying 2d points in angles. Its points can only be on the surface of the
 * sun. It could also be regarded as a scecial case of a 3d coordinate system
 * with a fixed radius. THETA defines the vertical angle in radians from solar
 * north. PHI specifies the vertical angle from the sun-eye line.
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 * 
 */
public class SphericalCoordinateSystem extends AbstractCoordinateSystem implements CoordinateSystem {
    public static final int PHI = 0;
    public static final int THETA = 1;

    private CoordinateDimension phiDimension;
    private CoordinateDimension thetaDimension;

    public SphericalCoordinateSystem() {
        this(Unit.Radian);
    }

    public SphericalCoordinateSystem(Unit unit) {
        this(new GenericCoordinateDimension(unit, " phi"), new GenericCoordinateDimension(unit, "theta"));
    }

    public SphericalCoordinateSystem(CoordinateDimension phiDimension, CoordinateDimension thetaDimension) {
        this.phiDimension = phiDimension;
        this.thetaDimension = thetaDimension;
    }

    public int getDimensions() {
        return 2;
    }

    public CoordinateDimension getDimension(int dimension) {
        switch (dimension) {
        case PHI:
            return this.phiDimension;
        case THETA:
            return this.thetaDimension;
        default:
            throw new IllegalArgumentException("Illegal dimension Number " + dimension + " for Coordinate System " + this);
        }
    }

    public CoordinateConversion getConversion(CoordinateSystem coordinateSystem) {
        if (coordinateSystem instanceof SolarSphereCoordinateSystem) {
            return new SphericalToSolarSphereConversion(this, (SolarSphereCoordinateSystem) coordinateSystem);
        }

        return super.getConversion(coordinateSystem);
    }
}
