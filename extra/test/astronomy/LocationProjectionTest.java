package org.helioviewer.jhv.layers;

import java.util.Locale;
import java.util.TimeZone;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.astronomy.Spice;
import org.helioviewer.jhv.astronomy.SpiceMath;
import org.helioviewer.jhv.astronomy.Stars;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.wcs.ObserverGeometry;

import spice.basic.CSPICE;

// ViewpointLayer.projectLocation in J2000 against eqs. (3), (6) of Skokić in the Carrington frame, occultation, solar pole
public class LocationProjectionTest {

    private static final double AU = 149_597_870.7;
    private static final double C = 299_792.458;
    private static final double ARCSEC = Math.toRadians(1 / 3600.);
    private static final String[] PLANETS = {"MERCURY", "VENUS", "EARTH", "MARS BARYCENTER", "JUPITER BARYCENTER", "SATURN BARYCENTER", "URANUS BARYCENTER", "NEPTUNE BARYCENTER"};

    public static void main(String[] arguments) throws Exception {
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        AppInit.loadSpice();

        double[][] pole = CSPICE.pxform("J2000", "SOLO_IAU_SUN_2009", CSPICE.str2et("2026-10-20T00:00:00"));
        for (int i = 0; i < 3; i++)
            check(Math.abs(ViewpointLayer.SOLAR_POLE[i] - pole[2][i]) < 1e-12, "solar pole differs from the SOLO_IAU_SUN_2009 frame");
        placement(pole, Stars.load());
        occultation();
        System.out.println("PASS: Location planets and stars against the Carrington reference, occultation by the solar disk, solar pole");
    }

    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }

    private static double[] rotate(double[][] m, double[] v) {
        return new double[]{m[0][0] * v[0] + m[0][1] * v[1] + m[0][2] * v[2], m[1][0] * v[0] + m[1][1] * v[1] + m[1][2] * v[2], m[2][0] * v[0] + m[2][1] * v[1] + m[2][2] * v[2]};
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] unit(double[] v) {
        double n = Math.sqrt(dot(v, v));
        return new double[]{v[0] / n, v[1] / n, v[2] / n};
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] position(String observer, String target, String frame, String abcorr, double et) throws Exception {
        double[] v = new double[3], lt = new double[1];
        CSPICE.spkpos(target, et, frame, abcorr, observer, v, lt);
        return v;
    }

    private static Vec2 project(double[] v, boolean beyondSun, double[] sun, double[] pole, double distance) {
        double sunDistance = Math.sqrt(dot(sun, sun));
        double[] z = sun.clone(), north = new double[3], west = new double[3];
        ViewpointLayer.helioprojectiveAxes(z, sunDistance, pole, north, west);
        return ViewpointLayer.projectLocation(v, beyondSun, z, north, west, sunDistance, distance);
    }

    // helioprojective from spherical coordinates, https://celestialscenes.com/alma/convert/coordconvert.pdf eqs. (3), (6)
    private static Vec2 reference(double[] v, double[] sun, double distance) {
        double[] p = SpiceMath.recrad(v), s = SpiceMath.recrad(sun);
        double alpha = p[1], delta = p[2], alpha0 = s[1], delta0 = s[2];
        double phi = Math.atan2(Math.sin(alpha - alpha0), Math.tan(delta) * Math.cos(delta0) - Math.sin(delta0) * Math.cos(alpha - alpha0));
        double rho = Math.acos(Math.clamp(Math.cos(delta) * Math.cos(delta0) * Math.cos(alpha - alpha0) + Math.sin(delta) * Math.sin(delta0), -1, 1));
        return ObserverGeometry.helioprojectiveToHpcPlane(distance, Math.atan(-Math.tan(rho) * Math.sin(phi)), Math.asin(Math.clamp(Math.sin(rho) * Math.cos(phi), -1, 1)));
    }

    private static double offset(Vec2 a, Vec2 b, double distance) {
        return Math.hypot(a.x - b.x, a.y - b.y) / distance / ARCSEC;
    }

    private static void placement(double[][] pole, Stars.Catalog catalog) throws Exception {
        double worstPlanet = 0, worstStar = 0;
        for (String observer : new String[]{"EARTH", "SOLO", "VENUS"})
            for (String date : new String[]{"2021-03-01T00:00:00", "2026-10-20T00:00:00", "2029-06-01T00:00:00"}) {
                double et = CSPICE.str2et(date);
                long milli = TimeUtils.parse(date);
                double distance = Spice.getCarrington(observer, new JHVTime(milli)).distance;
                double[] sunJ = position(observer, "SUN", "J2000", "LT+S", et);
                double[] sunC = position(observer, "SUN", "SOLO_IAU_SUN_2009", "LT+S", et);

                for (String planet : PLANETS) {
                    if (planet.equals(observer))
                        continue;
                    Vec2 a = project(position(observer, planet, "J2000", "LT+S", et), false, sunJ, pole[2], distance);
                    double[] pC = position(observer, planet, "SOLO_IAU_SUN_2009", "LT+S", et);
                    if (dot(pC, sunC) <= 0) {
                        check(a == null, planet + " from " + observer + " behind the observer");
                        continue;
                    }
                    if (a == null)
                        continue; // behind the solar disk
                    worstPlanet = Math.max(worstPlanet, offset(a, reference(pC, sunC, distance), distance));
                }

                double[] sv = new double[6], lt = new double[1];
                CSPICE.spkezr(observer, et, "J2000", "NONE", "SSB", sv, lt);
                double[] hp = position("SUN", observer, "J2000", "NONE", et);
                double em = Math.sqrt(dot(hp, hp));
                double[] ssb = {sv[0] / AU, sv[1] / AU, sv[2] / AU}, velocity = {sv[3] / C, sv[4] / C, sv[5] / C};
                double years = Stars.julianYear(milli) - catalog.epoch();
                double[] z = unit(sunJ), sunP = rotate(pole, sunJ);
                double cosCone = Math.cos(Math.toRadians(30));
                for (Stars.Star s : catalog.stars()) {
                    if (s.x() * z[0] + s.y() * z[1] + s.z() * z[2] < cosCone)
                        continue;
                    double[] u = Stars.apparent(s, years, ssb, unit(hp), em / AU, velocity);
                    Vec2 a = project(u, true, sunJ, pole[2], distance);
                    if (a == null)
                        continue;
                    worstStar = Math.max(worstStar, offset(a, reference(rotate(pole, u), sunP, distance), distance));
                }
            }
        check(worstPlanet < 1e-6, "planet placement differs from the Carrington reference by " + worstPlanet + " arcsec");
        check(worstStar < 1e-6, "star placement differs from the reference by " + worstStar + " arcsec");
    }

    // direction at angle (in units of the solar angular radius) from the Sun, at the given distance
    private static double[] around(double[] sun, double radii, double distance) {
        double d = Math.sqrt(dot(sun, sun));
        double angle = radii * Math.asin(Sun.Radius / d);
        double[] z = unit(sun), x = unit(cross(z, new double[]{0, 0, 1}));
        double c = Math.cos(angle), s = Math.sin(angle);
        return new double[]{distance * (c * z[0] + s * x[0]), distance * (c * z[1] + s * x[1]), distance * (c * z[2] + s * x[2])};
    }

    private static final double[] POLE = {0, 0, 1};

    private static void occultation() {
        double[] sun = {30, -200, 15};
        double d = Math.sqrt(dot(sun, sun));
        check(project(new double[]{-sun[0], -sun[1], -sun[2]}, false, sun, POLE, d) == null, "behind the observer");
        check(project(new double[]{-sun[0], -sun[1], -sun[2]}, true, sun, POLE, d) == null, "star behind the observer");
        check(project(around(sun, 0, 1), true, sun, POLE, d) == null, "star at the Sun centre");
        check(project(around(sun, 0.99, 1), true, sun, POLE, d) == null, "star just inside the disk");
        check(project(around(sun, 1.01, 1), true, sun, POLE, d) != null, "star just outside the disk");
        check(project(around(sun, 0.5, 0.5 * d), false, sun, POLE, d) != null, "planet in transit");
        check(project(around(sun, 0.5, 2 * d), false, sun, POLE, d) == null, "planet behind the disk");
        check(project(around(sun, 1.01, 2 * d), false, sun, POLE, d) != null, "planet beyond the Sun, outside the disk");
    }
}
