package org.helioviewer.jhv.astronomy;

import java.nio.file.Path;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.time.JHVTime;

import spice.basic.CSPICE;
import spice.basic.KernelDatabase;
import spice.basic.SpiceErrorException;
import spice.basic.TDBTime;

//import com.google.common.base.Stopwatch;

public class Spice {

    public static void loadKernels(@Nonnull List<String> files) {
        try {
            for (String f : files) {
                KernelDatabase.load(Path.of(JHVGlobals.dataCacheDir, f).toString());
            }
        } catch (Exception e) {
            Log.error(e);
        }
    }

    @Nullable
    public static String dateParse2UTC(@Nonnull String date) {
        try {
            return new TDBTime(date).toUTCString("isoc", 0);
        } catch (SpiceErrorException e) {
            Log.error(e);
        }
        return null;
    }

    @Nullable
    static PositionCartesian[] getPositionRange(@Nonnull String observer, @Nonnull String target, Frame frame, long start, long end, long deltat) {
        // Stopwatch sw = Stopwatch.createStarted();
        try {
            long dt = deltat * 1000;
            PositionCartesian[] ret = new PositionCartesian[(int) ((end - start) / dt) + 1];
            int i = 0;
            for (long milli = start; milli <= end; milli += dt) {
                double[] v = positionRectangular(target, milli, frame.toString(), observer);
                ret[i++] = new PositionCartesian(milli, v[0], v[1], v[2]);
            }
            //System.out.println((sw.elapsed().toNanos() / 1e9));
            return ret;
        } catch (SpiceErrorException e) {
            Log.error(e);
        }
        return null;
    }

    @Nullable
    static Position getPositionLatitudinal(@Nonnull String observer, @Nonnull String target, Frame frame, JHVTime time) {
        try {
            double[] c = positionLatitudinal(target, time.milli, frame.toString(), observer);
            return new Position(time, c[0], c[1], c[2]);
        } catch (SpiceErrorException e) {
            Log.error(e);
        }
        return null;
    }

    @Nonnull
    public static Position getEarthCarrington(JHVTime time) {
        try {
            double[] c = positionLatitudinal("EARTH", time.milli, "SOLO_IAU_SUN_2003", "SUN");
            // like in SSW.getEarthSSW
            double lon = c[1];
            if (lon < 0)
                lon += 2 * Math.PI;
            return new Position(time, c[0], -lon, c[2]);
        } catch (Exception e) {
            Log.error(e);
        }
        return SSW.getEarthSSW(time);
    }

    private static final JHVTime J2000 = new JHVTime("2000-01-01T12:00:00");

    private static double milli2et(long milli) throws SpiceErrorException {
        double sec = (milli - J2000.milli) / 1000.;
        return sec + CSPICE.deltet(sec, "UTC");
    }

    private static long et2milli(double et) throws SpiceErrorException {
        double sec = et - CSPICE.deltet(et, "ET");
        return (long) (sec * 1000. + J2000.milli + .5);
    }

    private static double[] positionRectangular(String target, long milli, String frame, String observer) throws SpiceErrorException {
        double et = milli2et(milli);
        double[] lt = new double[1];
        double[] v = new double[3];
        CSPICE.spkpos(target, et, frame, "NONE", observer, v, lt);
        v[0] *= Sun.RadiusKMeterInv;
        v[1] *= Sun.RadiusKMeterInv;
        v[2] *= Sun.RadiusKMeterInv;
        return v;
    }

    private static double[] positionLatitudinal(String target, long milli, String frame, String observer) throws SpiceErrorException {
        return CSPICE.reclat(positionRectangular(target, milli, frame, observer));
    }

}
