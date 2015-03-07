package org.helioviewer.base.physics;

import java.util.Locale;

import org.helioviewer.base.math.MathUtils;

// TODO: Malte Nuhn - GET RID OF THESE CLASSES

public class StonyhurstHeliographicCoordinates implements SolarCoordinates {

    // origin is at the center of the sun
    // the angles are measured on the sun relative to the intersection of the
    // solar equator
    // and the central meridian as seen from earth

    // latitude, increasing towards solar north
    public final double theta;

    // longitude, increasing towards the solar west limb
    public final double phi;

    // radius
    public final double r;

    public StonyhurstHeliographicCoordinates(double newTheta, double newPhi, double newR) {
        theta = newTheta;
        phi = newPhi;
        r = newR;
    }

    public StonyhurstHeliographicCoordinates(SolarCoordinates solarCoordinates) {
        HeliocentricEarthEquatorialCoordinates coordinates = solarCoordinates.convertToHeliocentricEarthEquatorialCoordinates();
        r = Math.sqrt(coordinates.x * coordinates.x + coordinates.y * coordinates.y + coordinates.z * coordinates.z);
        theta = Math.atan(coordinates.z / Math.sqrt(coordinates.x * coordinates.x + coordinates.y * coordinates.y)) * MathUtils.radeg;
        phi = Math.atan2(coordinates.y, coordinates.x) * MathUtils.radeg;
    }

    public HeliocentricEarthEquatorialCoordinates convertToHeliocentricEarthEquatorialCoordinates() {
        double heeqX = r * Math.cos(theta / MathUtils.radeg) * Math.cos(phi / MathUtils.radeg);
        double heeqY = r * Math.cos(theta / MathUtils.radeg) * Math.sin(phi / MathUtils.radeg);
        double heeqZ = r * Math.sin(theta / MathUtils.radeg);
        return new HeliocentricEarthEquatorialCoordinates(heeqX, heeqY, heeqZ);
    }

    public String toString() {
        return String.format(Locale.ENGLISH, "theta = %f, phi = %f, r = %f", theta, phi, r);
    }
}
