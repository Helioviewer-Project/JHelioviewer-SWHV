package org.helioviewer.gl3d.wcs.impl;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.wcs.Cartesian2DCoordinateSystem;
import org.helioviewer.gl3d.wcs.CoordinateConversion;
import org.helioviewer.gl3d.wcs.CoordinateSystem;
import org.helioviewer.gl3d.wcs.CoordinateVector;
import org.helioviewer.gl3d.wcs.IllegalCoordinateVectorException;
import org.helioviewer.gl3d.wcs.Unit;
import org.helioviewer.gl3d.wcs.conversion.SolarImageToSolarSphereConversion;
import org.helioviewer.gl3d.wcs.conversion.SolarImageToSphericalConversion;
import org.helioviewer.gl3d.wcs.conversion.SolarImageToTextureConversion;

/**
 * The {@link SolarImageCoordinateSystem} is used as a representation for points
 * on solar images and provides various {@link CoordinateConversion}s to convert
 * these points into other {@link CoordinateSystem}s. It's center is at the
 * center of the sun, the units are in kilometers.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class SolarImageCoordinateSystem extends Cartesian2DCoordinateSystem {
    private double solarRadiusSquare;

    public SolarImageCoordinateSystem() {
        super(Unit.Kilometer);
        this.solarRadiusSquare = this.getSolarRadius() * this.getSolarRadius();
    }

    public CoordinateConversion getConversion(CoordinateSystem coordinateSystem) {
        if (coordinateSystem instanceof SolarSphereCoordinateSystem) {
            return new SolarImageToSolarSphereConversion(this, (SolarSphereCoordinateSystem) coordinateSystem);
        } else if (coordinateSystem.getClass().isAssignableFrom(TextureCoordinateSystem.class)) {
            return new SolarImageToTextureConversion(this, (TextureCoordinateSystem) coordinateSystem);
        } else if (coordinateSystem.getClass().isAssignableFrom(SphericalCoordinateSystem.class)) {
            return new SolarImageToSphericalConversion(this, (SphericalCoordinateSystem) coordinateSystem);
        }

        return super.getConversion(coordinateSystem);
    }

    public boolean isInsideDisc(CoordinateVector vector) {
        if (!getClass().isAssignableFrom(vector.getCoordinateSystem().getClass())) {
            throw new IllegalCoordinateVectorException("Cannot handle Vector, CoordinateSystems do not match!");
        }
        double x = vector.getValue(X_COORDINATE);
        double y = vector.getValue(Y_COORDINATE);
        return ((x * x + y * y) <= this.solarRadiusSquare);
    }

    public double getSolarRadius() {
        return Constants.SunRadius;
    }

    public double getSolarRadiusSquare() {
        return this.solarRadiusSquare;
    }

}
