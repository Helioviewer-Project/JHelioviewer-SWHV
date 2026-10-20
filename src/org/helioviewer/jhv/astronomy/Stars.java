package org.helioviewer.jhv.astronomy;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

import org.helioviewer.jhv.io.FileUtils;
import org.helioviewer.jhv.time.TimeUtils;

import org.jastronomy.jsofa.JSOFA;

// Gaia stars bundled by extra/stars/update_stars.py
public final class Stars {

    private static final String RESOURCE = "/data/stars.bin";
    private static final double JYEAR_MILLI = 365.25 * 86400 * 1000;
    private static final double MAS = Math.toRadians(1 / 3.6e6);

    // ra, dec [rad], pmra = dRA/dt, pmdec [rad/yr], parallax [arcsec], rv [km/s], ICRS unit vector at the catalogue epoch
    public record Star(double ra, double dec, double pmra, double pmdec, double parallax, double rv, float mag, int hip,
                       double x, double y, double z) {}

    public record Catalog(double epoch /* Julian year */, Star[] stars) {}

    public static Catalog load() throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(FileUtils.getResource(RESOURCE)))) {
            byte[] magic = new byte[8];
            in.readFully(magic);
            if (!Arrays.equals(magic, "JHVSTARS".getBytes()) || in.readInt() != 1)
                throw new IOException("Unknown star catalogue format");
            double epoch = in.readDouble();
            Star[] stars = new Star[in.readInt()];
            for (int i = 0; i < stars.length; i++) {
                double ra = Math.toRadians(in.readDouble());
                double dec = Math.toRadians(in.readDouble());
                double cosDec = Math.cos(dec);
                double pmra = in.readFloat() * MAS / cosDec;
                double pmdec = in.readFloat() * MAS;
                double parallax = in.readFloat() / 1000.;
                double rv = in.readFloat();
                float mag = in.readFloat();
                int hip = in.readInt();
                stars[i] = new Star(ra, dec, pmra, pmdec, parallax, rv, mag, hip, cosDec * Math.cos(ra), cosDec * Math.sin(ra), Math.sin(dec));
            }
            return new Catalog(epoch, stars);
        }
    }

    public static double julianYear(long milli) {
        return 2000 + (milli - TimeUtils.J2000.milli) / JYEAR_MILLI;
    }

    // apparent ICRS-aligned direction from the observer, iauAtciq without the precession-nutation rotation:
    // ssb = SSB->observer [au], sun = Sun->observer unit vector, sunDistance [au], velocity = barycentric velocity [c]
    public static double[] apparent(Star star, double years, double[] ssb, double[] sun, double sunDistance, double[] velocity) {
        double[] p = JSOFA.jauPmpx(star.ra, star.dec, star.pmra, star.pmdec, star.parallax, star.rv, years, ssb);
        p = JSOFA.jauLdsun(p, sun, sunDistance);
        double bm1 = Math.sqrt(1 - (velocity[0] * velocity[0] + velocity[1] * velocity[1] + velocity[2] * velocity[2]));
        return JSOFA.jauAb(p, velocity, sunDistance, bm1);
    }

    private Stars() {}
}
