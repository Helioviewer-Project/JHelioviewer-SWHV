package org.helioviewer.jhv.astronomy;

import java.util.Locale;
import java.util.TimeZone;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.time.TimeUtils;

import org.jastronomy.jsofa.JSOFA;

import spice.basic.CSPICE;

// bundled catalogue, Stars.apparent against SOFA, barycentric state range against spkezr
public class StarsTest {

    private static final double AU = 149_597_870.7;
    private static final double C = 299_792.458;
    private static final double DAY = 86400;
    private static final double ARCSEC = Math.toRadians(1 / 3600.);
    private static final String[] OBSERVERS = {"EARTH", "SOLO"};
    private static final String[] DATES = {"2021-03-01T00:00:00", "2026-10-20T00:00:00", "2029-06-01T00:00:00"};

    public static void main(String[] arguments) throws Exception {
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        AppInit.loadSpice();

        Stars.Catalog catalog = catalogue();
        chain(catalog);
        state();
        System.out.println("PASS: star catalogue, apparent star directions against SOFA, barycentric state against spkezr");
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }

    private static Stars.Catalog catalogue() throws Exception {
        Stars.Catalog catalog = Stars.load();
        Stars.Star[] stars = catalog.stars();
        check(stars.length > 100_000, "catalogue has " + stars.length + " stars");
        check(catalog.epoch() > 2000 && catalog.epoch() < 2100, "catalogue epoch " + catalog.epoch());
        int hip = 0;
        for (int i = 0; i < stars.length; i++) {
            Stars.Star s = stars[i];
            check(i == 0 || stars[i - 1].mag() <= s.mag(), "catalogue sorted by magnitude at " + i);
            double norm = s.x() * s.x() + s.y() * s.y() + s.z() * s.z();
            check(Math.abs(norm - 1) < 1e-12, "unit vector at " + i);
            check(Double.isFinite(s.pmra()) && Double.isFinite(s.pmdec()) && Double.isFinite(s.parallax()) && Double.isFinite(s.rv()), "finite astrometry at " + i);
            if (s.hip() > 0)
                hip++;
        }
        check(hip > 50_000, "catalogue has " + hip + " HIP numbers");
        return catalog;
    }

    // jauApcs + jauAtciq with an identity precession-nutation matrix and the catalogue interval
    private static void chain(Stars.Catalog catalog) throws Exception {
        double worst = 0;
        for (String observer : OBSERVERS)
            for (String date : DATES) {
                double et = CSPICE.str2et(date);
                double[] sv = new double[6], hp = new double[3], lt = new double[1];
                CSPICE.spkezr(observer, et, "J2000", "NONE", "SSB", sv, lt);
                CSPICE.spkpos(observer, et, "J2000", "NONE", "SUN", hp, lt);

                double[] ssb = {sv[0] / AU, sv[1] / AU, sv[2] / AU};
                double[] velocity = {sv[3] / C, sv[4] / C, sv[5] / C};
                double em = Math.sqrt(hp[0] * hp[0] + hp[1] * hp[1] + hp[2] * hp[2]);
                double[] sun = {hp[0] / em, hp[1] / em, hp[2] / em};
                double years = Stars.julianYear(TimeUtils.parse(date)) - catalog.epoch();

                JSOFA.Astrom astrom = new JSOFA.Astrom();
                double[][] ebpv = {ssb, {sv[3] / AU * DAY, sv[4] / AU * DAY, sv[5] / AU * DAY}};
                JSOFA.jauApcs(2451545.0, et / DAY, new double[2][3], ebpv, new double[]{hp[0] / AU, hp[1] / AU, hp[2] / AU}, astrom);
                astrom.bpn = new double[][]{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
                astrom.pmt = years; // jauApcs counts from J2000

                for (Stars.Star s : catalog.stars()) {
                    double[] p = Stars.apparent(s, years, ssb, sun, em / AU, velocity);
                    JSOFA.SphericalCoordinate rd = JSOFA.jauAtciq(s.ra(), s.dec(), s.pmra(), s.pmdec(), s.parallax(), s.rv(), astrom);
                    double cd = Math.cos(rd.delta);
                    double[] q = {cd * Math.cos(rd.alpha), cd * Math.sin(rd.alpha), Math.sin(rd.delta)};
                    double cx = p[1] * q[2] - p[2] * q[1], cy = p[2] * q[0] - p[0] * q[2], cz = p[0] * q[1] - p[1] * q[0];
                    worst = Math.max(worst, Math.atan2(Math.sqrt(cx * cx + cy * cy + cz * cz), p[0] * q[0] + p[1] * q[1] + p[2] * q[2]));
                }
            }
        check(worst < 1e-6 * ARCSEC, "star chain differs from SOFA by " + worst / ARCSEC + " arcsec");
    }

    // samples and interpolation between them
    private static void state() throws Exception {
        long start = TimeUtils.parse("2026-10-20T00:00:00"), end = TimeUtils.parse("2026-10-20T01:00:00"), mid = TimeUtils.parse("2026-10-20T00:30:30");
        double et = CSPICE.str2et("2026-10-20T00:30:30");
        double[] pos = new double[3], vel = new double[3], sv = new double[6], lt = new double[1];
        PositionResponse.Interpolated out = new PositionResponse.Interpolated();
        for (String target : OBSERVERS) {
            PositionResponse response = Spice.getRange("SSB", target, "J2000", "NONE", true, start, end, 60);
            check(response != null, "state range for " + target);
            response.interpolateState(mid, start, end, pos, vel, out);
            CSPICE.spkezr(target, et, "J2000", "NONE", "SSB", sv, lt);
            double dp = 0, dv = 0;
            for (int i = 0; i < 3; i++) {
                dp = Math.max(dp, Math.abs(pos[i] / Sun.RadiusKMeterInv - sv[i]));
                dv = Math.max(dv, Math.abs(vel[i] / Spice.VelocityFactor - sv[3 + i]));
            }
            check(dp < 0.01, target + " position differs by " + dp + " km");
            check(dv < 1e-5, target + " velocity differs by " + dv + " km/s");
        }

        PositionResponse position = Spice.getRange("SSB", "EARTH", "J2000", "NONE", false, start, end, 60);
        boolean refused = false;
        try {
            position.interpolateState(mid, start, end, pos, vel, out);
        } catch (IllegalStateException e) {
            refused = true;
        }
        check(refused, "position response refuses interpolateState");
    }
}
