package org.helioviewer.base.physics;

import java.util.Calendar;
import java.util.Locale;

import org.helioviewer.base.math.Vector2dDouble;

public class HeliocentricCartesianCoordinatesFromEarth implements SolarCoordinates {

    // the origin is at the center of the sun

    // along the observer - sun line, pointing toward the observer
    public final double z;

    // perpendicular to the z-axis and in the plane containing both the z axis
    // and the solar rotation axis,
    // with y increasing to solar North
    public final double y;

    // perpendicular to the y-axis and z-axis, increasing towards solar West
    public final double x;

    // heliocentric cartesian coordinates depend on the location of the observer
    // currently we only support the observer being approximately on the earth
    // in order to convert to HEEQ, we need the time of the observation to
    // correct for B0
    public final Calendar observationTime;

    public HeliocentricCartesianCoordinatesFromEarth(double newX, double newY, double newZ, Calendar newObservationTime) {
        x = newX;
        y = newY;
        z = newZ;
        observationTime = newObservationTime;
    }

    // constructor for points on the solar disk as seen on images
    public HeliocentricCartesianCoordinatesFromEarth(Vector2dDouble physicalCoordinates, Calendar newObservationTime) {
        x = physicalCoordinates.getX();
        y = physicalCoordinates.getY();
        z = Math.sqrt(Constants.SunRadius * Constants.SunRadius - x * x - y * y);
        observationTime = newObservationTime;
    }

    public HeliocentricCartesianCoordinatesFromEarth(SolarCoordinates solarCoordinates, Calendar newObservationTime) {
        HeliocentricEarthEquatorialCoordinates coordinates = solarCoordinates.convertToHeliocentricEarthEquatorialCoordinates();
        observationTime = newObservationTime;
        x = coordinates.y;
        double b0 = Astronomy.getB0InRadians(observationTime);
        y = coordinates.z * Math.cos(b0) - coordinates.x * Math.sin(b0);
        z = coordinates.z * Math.sin(b0) + coordinates.x * Math.cos(b0);
    }

    public HeliocentricEarthEquatorialCoordinates convertToHeliocentricEarthEquatorialCoordinates() {
        double b0 = Astronomy.getB0InRadians(observationTime);
        double heeqX = z * Math.cos(b0) - y * Math.sin(b0);
        double heeqY = x;
        double heeqZ = z * Math.sin(b0) + y * Math.cos(b0);
        return new HeliocentricEarthEquatorialCoordinates(heeqX, heeqY, heeqZ);
    }

    public Vector2dDouble getCartesianCoordinatesOnDisc() {
        return new Vector2dDouble(x, y);
    }

    public String toString() {
        return String.format(Locale.ENGLISH, "x = %f, y = %f, z = %f  (time: %tc)", x, y, z, observationTime);
    }
}
