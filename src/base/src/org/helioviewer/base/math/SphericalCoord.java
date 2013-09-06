package org.helioviewer.base.math;

import java.util.Locale;

public class SphericalCoord {

    public double theta = 0.0;
    public double phi = 0.0;
    public double r = 0.0;

    public SphericalCoord(double theta, double phi, double r) {
        this.theta = theta;
        this.phi = phi;
        this.r = r;
    }

    public SphericalCoord() {
    }

    public SphericalCoord(SphericalCoord stony) {
        this(stony.theta, stony.phi, stony.r);
    }

    public String toString() {
        return String.format(Locale.ENGLISH, "Theta=%.2f¡, Phi=%.2f¡, r=%.2f", theta, phi, r);
    }

    public boolean equals(Object otherObject) {
        if (otherObject instanceof SphericalCoord) {
            SphericalCoord otherCoord = (SphericalCoord) otherObject;
            return (otherCoord.theta == this.theta && otherCoord.phi == this.phi && otherCoord.r == this.r);
        } else {
            return false;
        }
    }

}
