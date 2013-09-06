package org.helioviewer.jhv.plugins.hekplugin.math;

import java.util.Date;

import org.helioviewer.base.math.CartesianCoord;
import org.helioviewer.base.math.MathUtils;
import org.helioviewer.base.math.SphericalCoord;
import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.base.math.Vector3dDouble;
import org.helioviewer.base.physics.Constants;

/**
 * This class copes all coordinate transformations needed in JHV.
 * <p>
 * The old transformations are errornous and to be abolished, because they are
 * very confusing, since they are spread of too many classes
 * <p>
 * IT IS CURRENTLY HEAVILY WORKED ON - SO DON'T TRUST IT! IT IS ALSO NOT WELL
 * DOCUMENTED! SIMPLY IGNORE THIS FILE!
 * 
 * @author Malte Nuhn
 * 
 */
public class HEKCoordinateTransform {

    /**
     * How many radians does the coordinate rotate?
     * 
     * @param latitude
     * @param timeDifferenceInSeconds
     * @return
     */
    public static double calculateRotationInRadians(double latitude, double timeDifferenceInSeconds) {
        /*
         * sin2l = sin(latitude)^2 sin4l = sin2l*sin2l rotation =
         * 1.e-6*dt_rot*(2.894-0.428*sin2l-0.37*sin4l)*180/pi.
         * 
         * from rotation rate of small magnetic features (Howard, Harvey, and
         * Forgach, Solar Physics, 130, 295, 1990)
         */
        double sin2l = Math.sin(latitude);
        sin2l = sin2l * sin2l;
        double sin4l = sin2l * sin2l;
        return 1.0e-6 * timeDifferenceInSeconds * (2.894 - 0.428 * sin2l - 0.37 * sin4l);
    }

    /**
     * How many degrees does the coordinate rotate?
     * 
     * @param latitude
     * @param timeDifferenceInSeconds
     * @return
     */
    private static double calculateRotationInDegree(double latitude, double timeDifferenceInSeconds) {
        return calculateRotationInRadians(latitude, timeDifferenceInSeconds) * 180.0 / Math.PI;
    }

    /**
     * Rotanional Spherical Coordinates, where the Z axis is aligned to the
     * NORTH / SOUTH axis
     * 
     * This takes Spherical Coordinates, Transforms it to Cartesian, Switches
     * Axes,
     * 
     * result.x = input.z; result.y = input.x; result.z = input.y;
     * 
     * And transforms back to Spherical Coordinates
     * 
     * @param stony
     * @return
     */
    public static SphericalCoord StonyhurstToRotational(SphericalCoord stony) {

        CartesianCoord cart = StonyhurstToHeliocentricCartesian(stony, 0.0, 0.0);
        // switch axis!
        CartesianCoord rotational = new CartesianCoord();
        rotational.x = cart.z;
        rotational.y = cart.x;
        rotational.z = cart.y;

        SphericalCoord result = CartesianToSpherical(rotational);

        return result;

    }

    /**
     * Inverse operation...
     * 
     * @param stony
     * @return
     */
    public static SphericalCoord RotationalToStonyhurst(SphericalCoord rotational) {

        CartesianCoord cart = StonyhurstToHeliocentricCartesian(rotational, 0.0, 0.0);
        // switch axis!
        CartesianCoord stony = new CartesianCoord();
        stony.x = cart.y;
        stony.y = cart.z;
        stony.z = cart.x;

        SphericalCoord result = CartesianToSpherical(stony);

        return result;

    }

    public static SphericalCoord CartesianToSpherical(Vector3dDouble cart) {
        return CartesianToSpherical(new CartesianCoord(cart));
    }

    public static SphericalCoord CartesianToSpherical(CartesianCoord cart) {
        CartesianCoord swapped = new CartesianCoord();

        swapped.x = cart.y;
        swapped.y = cart.z;
        swapped.z = cart.x;

        SphericalCoord result = new SphericalCoord();
        result.r = Math.sqrt(swapped.x * swapped.x + swapped.y * swapped.y + swapped.z * swapped.z);
        result.theta = Math.atan(swapped.z / Math.sqrt(swapped.x * swapped.x + swapped.y * swapped.y)) * MathUtils.radeg;
        result.phi = Math.atan2(swapped.y, swapped.x) * MathUtils.radeg;
        return result;
    }

    public static SphericalCoord StonyhurstRotateStonyhurst(SphericalCoord stony, double timeDifferenceInSeconds) {
        double latitude = stony.theta / 180.0 * Math.PI;
        double degrees = calculateRotationInDegree(latitude, timeDifferenceInSeconds);

        SphericalCoord result = new SphericalCoord(stony);

        result.phi += degrees;
        result.phi %= 360.0; // was 180, but does this make sense?

        return result;
    }

    /**
     * Note: This is related to the current point of observation
     * 
     * @param stony
     * @return
     */
    public static CartesianCoord StonyhurstToHeliocentricCartesian(SphericalCoord stony, double bzero, double phizero) {

        CartesianCoord result = new CartesianCoord();
        result.x = stony.r * Math.cos(stony.theta / MathUtils.radeg) * Math.sin((stony.phi - phizero) / MathUtils.radeg);
        result.y = stony.r * (Math.sin(stony.theta / MathUtils.radeg) * Math.cos(bzero / MathUtils.radeg) - Math.cos(stony.theta / MathUtils.radeg) * Math.cos((stony.phi - phizero) / MathUtils.radeg) * Math.sin(bzero / MathUtils.radeg));
        result.z = stony.r * (Math.sin(stony.theta / MathUtils.radeg) * Math.sin(bzero / MathUtils.radeg) + Math.cos(stony.theta / MathUtils.radeg) * Math.cos((stony.phi - phizero) / MathUtils.radeg) * Math.cos(bzero / MathUtils.radeg));
        return result;
    }

    /**
     * @param helioCentricFromSat
     * @return
     */
    // public SphericalCoord
    // HeliocentricCartesiantocorrectHeliocentricCartesian(SphericalCoord
    // helioCentricFromSat) {
    /*
     * HeliocentricEarthEquatorialCoordinates coordinates =
     * solarCoordinates.convertToHeliocentricEarthEquatorialCoordinates();
     * observationTime = newObservationTime; x = coordinates.y; double b0 =
     * Astronomy.getB0InRadians(observationTime); y = coordinates.z *
     * Math.cos(b0) - coordinates.x * Math.sin(b0); z = coordinates.z *
     * Math.sin(b0) + coordinates.x * Math.cos(b0);
     */

    // }

    /*
     * HELIOCENTRIC EARTH EQUATORIAL (X,Y,Z)
     * 
     * Direct transformation from HELIOGRAPHIC STONYHURST
     * 
     * X = r cos(theta) cos(phi) Y = r cos(theta) sin(phi) Z = r sin(theta)
     * 
     * HOLDS ONLY IF FOR NON-TERRESTRIAL OBSERVERS, THE ORIGIN WILL STILL BE
     * REFERENCED TO THE CENTRAL MERIDIAN AS SEEN FROM EARTH!
     */

    public static boolean isVisibleStonyhurst(double theta, double phi) {
        if (phi > 90 || phi < -90)
            return false;
        return true;
    }

    public static CartesianCoord HelioProjectiveCartesianToHelioCentricCartesian(double thetax, double thetay) {
        CartesianCoord result = new CartesianCoord();
        result.x = Constants.SunMeanDistanceToEarth * Math.cos(thetay / MathUtils.radeg) * Math.sin(thetax / MathUtils.radeg) / Constants.SunRadius;
        result.y = Constants.SunMeanDistanceToEarth * Math.sin(thetay / MathUtils.radeg) / Constants.SunRadius;
        result.z = 1 - Math.cos(thetay / MathUtils.radeg) * Math.cos(thetax / MathUtils.radeg);
        return result;
    }

    public static boolean isVisibleCarrington(double theta, double phi, Date date, Date now) {
        // phi is in carrington coordinates
        double days = (date.getTime() - 788918400000l) / (1000.0 * 3600.0 * 24.0);
        phi = phi + 349.03 - (360.0 * days / 27.2753);

        while (phi < 0) {
            phi += 360.0;
        }

        phi -= 180;

        // return isVisibleStonyhurst(theta,phi,date,now);

        return true;
    }

    // UTC-HPR-TOPO [Helioprojective];
    public static Vector2dDouble convertHelioprojective(double theta1, double theta2) {
        return new Vector2dDouble(Math.sin(theta1 / MathUtils.radeg) * 1000, Math.sin(theta2 / MathUtils.radeg));
    }

    // UTC-HCR-TOPO[Heliocentric radial]
    public static Vector2dDouble fromRadial(double rho, double phi) {
        // umwandeln in
        return new Vector2dDouble(0, 0);
    }

    public static boolean isVisibleHelioprojective(double theta1, double theta2) {
        return false;
    }

    public static boolean isVisibleRadial(double rho, double phi) {
        return false;
    }

    // UTC-HGC-TOPO[Heliographic Carrington];
    public static SphericalCoord CarringtonToStonyhurst(SphericalCoord carrington, Date now) {

        /*
         * CARRINGTON COORDINATE SYSTEM IS A VARIANT OF THE HELIOGRAPHIC SYSTEM
         * WHICH ROTATES AT AN APPROXIMATION OF THE MEAN SOLAR ROTATIONAL RATE,
         * ASE ORIGINALLY USED BY CARRINGTON
         */

        /*
         * OLD mean Carrington Longitude in Degrees OLD = 349.03 - (360.* X /
         * 27.2753), http://umtof.umd.edu/pm/crn/CARRTIME.HTML where X is the
         * number of days since 1 January 1995. It is understood that OLD is to
         * be taken modulo 360. Note that the Carrington longitude decreases as
         * time increases. If one now compares the values of OLD with the values
         * listed in the Almanac one finds reasonable agreement, with maximum
         * discrepancies of about 4 hours.
         * 
         * 
         * Epoch timestamp: 788918400 Human time: Sun, 01 Jan 1995 00:00:00 GMT
         */

        // System.out.println("Phi OLD " + carrington.phi);

        // phi is in carrington coordinates
        double days = (now.getTime() - 788918400000l) / (1000.0 * 3600.0 * 24.0);
        carrington.phi += -349.03 + (360.0 * days / 27.2753);

        carrington.phi = makePhi(carrington.phi);
        return carrington;
    }

    public static boolean stonyIsVisible(SphericalCoord stony) {
        stony.phi = makePhi(stony.phi);
        if (stony.phi < -90 || stony.phi > 90)
            return false;
        return true;
    }

    public static double makePhi(double phi) {
        phi += 180.0;
        phi %= 360.0;
        phi -= 180.0;
        return phi;
    }

}
