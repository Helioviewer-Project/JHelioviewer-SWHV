package org.helioviewer.base.math;

public class SphericalCoordinates {

    public double elevation;

    public double azimuth;

    public SphericalCoordinates() {
        this.elevation = 0.0;
        this.azimuth = 0.0;
    }

    public SphericalCoordinates(double elevation, double azimuth) {
        this.elevation = elevation;
        this.azimuth = azimuth;
    }
}
