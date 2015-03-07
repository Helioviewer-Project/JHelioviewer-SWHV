package org.helioviewer.base.physics;

public class DifferentialRotation {

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

    private static double calculateRotationInDegree(double latitude, double timeDifferenceInSeconds) {
        return calculateRotationInRadians(latitude, timeDifferenceInSeconds) * 180.0 / Math.PI;
    }

    public static StonyhurstHeliographicCoordinates calculateNextPosition(StonyhurstHeliographicCoordinates currenPosition, double timeDifferenceInSeconds) {
        double rotation = calculateRotationInDegree(currenPosition.theta, timeDifferenceInSeconds);
        double newPhi = currenPosition.phi + rotation;
        newPhi = newPhi % 360.0;
        return new StonyhurstHeliographicCoordinates(currenPosition.theta, newPhi, currenPosition.r);
    }
}
