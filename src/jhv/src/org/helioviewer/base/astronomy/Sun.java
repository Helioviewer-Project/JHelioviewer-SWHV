package org.helioviewer.base.astronomy;

public class Sun {

    public static final double Radius = 1;
    public static final double Radius2 = Radius * Radius;
    public static final double RadiusMeter = 6.96e8;
    public static final double MeanEarthDistance = 149597870700.0;

    public static double calculateRotationInRadians(double latitude, double deltaTsec) {
        /*
         * sin2l = sin(latitude)^2 sin4l = sin2l*sin2l rotation =
         * 1.e-6*dt_rot*(2.894-0.428*sin2l-0.37*sin4l)*180/pi.
         *.
         * from rotation rate of small magnetic features (Howard, Harvey, and
         * Forgach, Solar Physics, 130, 295, 1990)
         */

        double sin2l = Math.sin(latitude);
        sin2l = sin2l * sin2l;
        double sin4l = sin2l * sin2l;
        return 1.0e-6 * deltaTsec * (2.894 - 0.428 * sin2l - 0.37 * sin4l);
    }

}
