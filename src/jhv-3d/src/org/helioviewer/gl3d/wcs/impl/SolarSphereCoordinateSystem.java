package org.helioviewer.gl3d.wcs.impl;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.wcs.Cartesian3DCoordinateSystem;
import org.helioviewer.gl3d.wcs.CoordinateConversion;
import org.helioviewer.gl3d.wcs.CoordinateSystem;
import org.helioviewer.gl3d.wcs.Unit;
import org.helioviewer.gl3d.wcs.conversion.SolarSphereToSolarImageConversion;
import org.helioviewer.gl3d.wcs.conversion.SolarSphereToStonyhurstHeliographicConversion;

/**
 * The 3-dimensional coordinate system that is used for the 3D representations
 * of the solar images. It is also the coordinate system the cameras operate in.
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 * 
 */
public class SolarSphereCoordinateSystem extends Cartesian3DCoordinateSystem {
    public SolarSphereCoordinateSystem() {
        super(Unit.Kilometer);
    }

    public CoordinateConversion getConversion(CoordinateSystem coordinateSystem) {
        if (coordinateSystem instanceof SolarImageCoordinateSystem) {
            return new SolarSphereToSolarImageConversion(this, (SolarImageCoordinateSystem) coordinateSystem);
        } else if (coordinateSystem instanceof StonyhurstHeliographicCoordinateSystem) {
            return new SolarSphereToStonyhurstHeliographicConversion(this, (StonyhurstHeliographicCoordinateSystem) coordinateSystem);
        }

        return super.getConversion(coordinateSystem);
    }

    public double getSolarRadius() {
        return Constants.SunRadius;
    }
}
