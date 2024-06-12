package org.helioviewer.jhv.astronomy;

import java.nio.file.Path;
import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Log;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeUtils;

//import com.google.common.base.Stopwatch;
import spice.basic.CSPICE;
import spice.basic.SpiceErrorException;

public class Spice {

    public static void loadKernels(List<String> files) throws SpiceErrorException {
        for (String f : files) {
            CSPICE.furnsh(Path.of(JHVGlobals.dataCacheDir, f).toString());
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
            double[] v = new double[3];
            for (long milli = start; milli <= end; milli += dt) {
                positionRec(observer, target, frame, milli, v);
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
            double[] c = positionLat(observer, target, frame, time.milli);
            return new Position(time, c[0], c[1], c[2]);
        } catch (SpiceErrorException e) {
            Log.error(e);
        }
        return null;
    }

    @Nullable
    public static Position getCarrington(String target, JHVTime time) {
        try {
            double[] c = positionRad("SUN", target, "SOLO_IAU_SUN_2009", time.milli);
            return new Position(time, c[0], -c[1], c[2]);
        } catch (Exception e) {
            Log.error(e);
        }
        return null;
    }

    private static final int[] axes = new int[]{3, 2, 1};

    @Nullable
    public static double[] getEulerRotation(String fromFrame, String toFrame, JHVTime time) {
        try {
            double et = milli2et(time.milli);
            return CSPICE.m2eul(CSPICE.pxform(fromFrame, toFrame, et), axes);
        } catch (Exception e) {
            Log.error(e);
        }
        return null;
    }

// Stars start

    private static final double[] zaxis = new double[]{0x1.f528efd03d6ddp-4, -0x1.b139ceec78046p-2, 0x1.cbac0fc6ffd8cp-1}; // IAU_SUN (0,0,1) in J2000

    @Nullable
    public static double[][] twovecSun(String target, JHVTime time) {
        try {
            double et = milli2et(time.milli);

            double[] xaxis = new double[3];
            CSPICE.spkpos(target, et, "J2000", "NONE", "SUN", xaxis, lightTimeUnused);
            //double[] zaxis = CSPICE.mxv(CSPICE.pxform("IAU_SUN", "J2000", et), new double[]{0, 0, 1});
            //System.out.println(">>> " + String.format("%a ,%a, %a", zaxis[0], zaxis[1], zaxis[2]));
            return CSPICE.twovec(xaxis, 1, zaxis, 3);
        } catch (Exception e) {
            Log.error(e);
        }
        return null;
    }

    @Nullable
    public static double[] radRotate(double ra, double dec, double[][] m) {
        try {
            return rotate2Rad(m, SpiceMath.latrec(1, ra, dec));
        } catch (Exception e) {
            Log.error(e);
        }
        return null;
    }

    @Nullable
    public static double[] posRad(String observer, String target, JHVTime time) {
        try {
            double[] v = new double[3];
            positionRec(observer, target, "J2000", time.milli, v);
            return SpiceMath.recrad(v);
        } catch (Exception e) {
            Log.error(e);
        }
        return null;
    }

    @Nullable
    public static double[] posRadM(String observer, String target, JHVTime time, double[][] m) {
        try {
            double[] v = new double[3];
            positionRec(observer, target, "J2000", time.milli, v);
            return rotate2Rad(m, v);
        } catch (Exception e) {
            Log.error(e);
        }
        return null;
    }

    private static double[] rotate2Rad(double[][] m, double[] v) throws SpiceErrorException {
        return SpiceMath.recrad(SpiceMath.mxv(m, v));
    }

// Stars end

    private static double milli2et(long milli) throws SpiceErrorException {
        double sec = (milli - TimeUtils.J2000.milli) / 1000.;
        return sec + CSPICE.deltet(sec, "UTC");
    }

    private static long et2milli(double et) throws SpiceErrorException {
        double sec = et - CSPICE.deltet(et, "ET");
        return (long) (sec * 1000. + TimeUtils.J2000.milli + .5);
    }

    private static final double[] lightTimeUnused = new double[1];

    private static void positionRec(String observer, String target, String frame, long milli, double[] result) throws SpiceErrorException {
        double et = milli2et(milli);
        CSPICE.spkpos(target, et, frame, "NONE", observer, result, lightTimeUnused);
        result[0] *= Sun.RadiusKMeterInv;
        result[1] *= Sun.RadiusKMeterInv;
        result[2] *= Sun.RadiusKMeterInv;
    }

    private static double[] positionLat(String observer, String target, String frame, long milli) throws SpiceErrorException {
        double[] v = new double[3];
        positionRec(observer, target, frame, milli, v);
        return SpiceMath.reclat(v);
    }

    private static double[] positionRad(String observer, String target, String frame, long milli) throws SpiceErrorException {
        double[] v = new double[3];
        positionRec(observer, target, frame, milli, v);
        return SpiceMath.recrad(v);
    }

}
