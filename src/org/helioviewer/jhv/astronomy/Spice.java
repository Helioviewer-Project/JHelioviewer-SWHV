package org.helioviewer.jhv.astronomy;

import java.nio.file.Path;
import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeUtils;

import spice.basic.CSPICE;
import spice.basic.SpiceErrorException;

//import com.google.common.base.Stopwatch;

public class Spice {

    public static void loadKernels(List<String> files) {
        try {
            for (String f : files) {
                CSPICE.furnsh(Path.of(JHVGlobals.dataCacheDir, f).toString());
            }
        } catch (SpiceErrorException e) {
            Log.error(e);
        }
    }

    @Nullable
    public static String timeParse2UTC(String time) {
        try {
            return CSPICE.et2utc(CSPICE.str2et(time), "isoc", 0);
        } catch (SpiceErrorException e) {
            Log.error(e);
        }
        return null;
    }

    @Nullable
    static Position.Cartesian[] getPositionRange(String observer, String target, String frame, long start, long end, long deltat) {
        // Stopwatch sw = Stopwatch.createStarted();
        try {
            long dt = deltat * 1000;
            Position.Cartesian[] ret = new Position.Cartesian[(int) ((end - start) / dt) + 1];
            int i = 0;
            for (long milli = start; milli <= end; milli += dt) {
                double[] v = positionRectangular(target, milli, frame, observer);
                ret[i++] = new Position.Cartesian(milli, v[0], v[1], v[2]);
            }
            //System.out.println((sw.elapsed().toNanos() / 1e9));
            return ret;
        } catch (SpiceErrorException e) {
            Log.error(e);
        }
        return null;
    }

    @Nullable
    static Position getPositionLatitudinal(String observer, String target, String frame, JHVTime time) {
        try {
            double[] c = positionLatitudinal(target, time.milli, frame, observer);
            return new Position(time, c[0], c[1], c[2]);
        } catch (SpiceErrorException e) {
            Log.error(e);
        }
        return null;
    }

    @Nullable
    public static Position getCarrington(String observer, String target, JHVTime time) {
        try {
            double[] c = positionLatitudinal(target, time.milli, "SOLO_IAU_SUN_2009", observer);
            // like in SSW.getEarthSSW
            double lon = c[1];
            if (lon < 0)
                lon += 2 * Math.PI;
            return new Position(time, c[0], -lon, c[2]);
        } catch (Exception e) {
            Log.error(e);
        }
        return null;
    }

    private static final int[] axes = new int[]{3, 2, 1};

    @Nullable
    public static double[] getRotation(String fromFrame, String toFrame, JHVTime time) {
        try {
            double et = milli2et(time.milli);
            return CSPICE.m2eul(CSPICE.pxform(fromFrame, toFrame, et), axes);
        } catch (Exception e) {
            Log.error(e);
        }
        return null;
    }

    private static double milli2et(long milli) throws SpiceErrorException {
        double sec = (milli - TimeUtils.J2000.milli) / 1000.;
        return sec + CSPICE.deltet(sec, "UTC");
    }

    private static long et2milli(double et) throws SpiceErrorException {
        double sec = et - CSPICE.deltet(et, "ET");
        return (long) (sec * 1000. + TimeUtils.J2000.milli + .5);
    }

    private static final double[] lightTimeUnused = new double[1];

    private static double[] positionRectangular(String target, long milli, String frame, String observer) throws SpiceErrorException {
        double et = milli2et(milli);
        double[] v = new double[3];
        CSPICE.spkpos(target, et, frame, "NONE", observer, v, lightTimeUnused);
        v[0] *= Sun.RadiusKMeterInv;
        v[1] *= Sun.RadiusKMeterInv;
        v[2] *= Sun.RadiusKMeterInv;
        return v;
    }

    private static double[] positionLatitudinal(String target, long milli, String frame, String observer) throws SpiceErrorException {
        return CSPICE.reclat(positionRectangular(target, milli, frame, observer));
    }

}
