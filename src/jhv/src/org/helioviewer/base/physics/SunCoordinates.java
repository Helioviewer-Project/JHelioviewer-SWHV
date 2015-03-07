package org.helioviewer.base.physics;

import org.helioviewer.base.math.SphericalCoordinates;

public class SunCoordinates extends SphericalCoordinates {

    public SunCoordinates() {
        super();
    }

    public SunCoordinates(double elevation, double azimuth) {
        super(elevation, azimuth);
    }
}