package org.helioviewer.jhv.astronomy;

import java.util.Locale;
import java.util.TimeZone;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.time.TimeUtils;

import spice.basic.CSPICE;

public class SpiceTest {

    public static void main(String[] arguments) throws Exception {
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        AppInit.loadSpice();
        ranges();

        double worst = 0;
        long start = TimeUtils.parse("1960-01-01T00:00:00"), end = TimeUtils.parse("2060-01-01T00:00:00");
        for (long milli = start; milli < end; milli += 3_600_001)
            worst = Math.max(worst, difference(milli));
        double[] table = CSPICE.gdpool("DELTET/DELTA_AT", 0, 1000);
        for (int i = 1; i < table.length; i += 2) {
            long leap = TimeUtils.J2000.milli + Math.round(table[i] * 1000);
            for (long d = -2000; d <= 2000; d++)
                worst = Math.max(worst, difference(leap + d));
        }
        if (worst > Math.ulp(2e9))
            throw new AssertionError("milli2et differs from deltet by " + worst + " s");
        System.out.println("PASS: position/state range coverage and UTC to ET against deltet, hourly 1960-2060 and around " + table.length / 2 + " leap seconds, worst " + worst + " s");
    }

    private static void ranges() throws Exception {
        long start = TimeUtils.parse("2026-08-12T15:25:05.554");
        PositionResponse.Interpolated out = new PositionResponse.Interpolated();
        double[] pos = new double[3], vel = new double[3], reference = new double[6], lt = new double[1];
        for (long span : new long[]{0, 1, 59999, 60000, 60001, 14515000}) {
            long end = start + span;
            for (boolean state : new boolean[]{false, true}) {
                PositionResponse response = Spice.getRange("EARTH", "MOON", "J2000", "LT+S", state, start, end, 60);
                if (response == null)
                    throw new AssertionError("No local range for span " + span + ", state " + state);
                if (state)
                    response.interpolateState(end, start, end, pos, vel, out);
                else
                    response.interpolateRectangular(end, start, end, pos, out);
                if (out.time != end)
                    throw new AssertionError("Local range shifted the requested end by " + (end - out.time) + " ms");
                CSPICE.spkezr("MOON", Spice.milli2et(end), "J2000", "LT+S", "EARTH", reference, lt);
                for (int i = 0; i < 3; i++) {
                    if (Math.abs(pos[i] / Sun.RadiusKMeterInv - reference[i]) > 0.01)
                        throw new AssertionError("Local range did not interpolate its final bracket");
                    if (state && Math.abs(vel[i] / Spice.VelocityFactor - reference[i + 3]) > 1e-5)
                        throw new AssertionError("Local state range did not interpolate its final bracket");
                }
            }
        }
    }

    private static double difference(long milli) throws Exception {
        double sec = (milli - TimeUtils.J2000.milli) / 1000.;
        double et = sec + CSPICE.deltet(sec, "UTC");
        return Math.abs(Spice.milli2et(milli) - et);
    }

}
