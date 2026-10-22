package org.helioviewer.jhv.astronomy;

import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeUtils;

//import com.google.common.base.Stopwatch;

import spice.basic.CSPICE;
import spice.basic.SpiceErrorException;
import spice.basic.SpiceException;

public class Spice {

    // DELTET constants of the leapseconds kernel, leaps [s past J2000 UTC]
    private record LeapSeconds(double deltaTA, double k, double eb, double m0, double m1, double[] deltaAT, double[] leaps) {}

    private static LeapSeconds leapSeconds; // kernels load at startup on the main thread

    public static void loadKernels(List<String> files) throws SpiceException {
        for (String f : files) {
            CSPICE.furnsh(f);
        }
        double[] m = CSPICE.gdpool("DELTET/M", 0, 2);
        double[] table = CSPICE.gdpool("DELTET/DELTA_AT", 0, 1000);
        double[] deltaAT = new double[table.length / 2], leaps = new double[table.length / 2];
        for (int i = 0; i < deltaAT.length; i++) {
            deltaAT[i] = table[2 * i];
            leaps[i] = table[2 * i + 1];
        }
        leapSeconds = new LeapSeconds(CSPICE.gdpool("DELTET/DELTA_T_A", 0, 1)[0], CSPICE.gdpool("DELTET/K", 0, 1)[0],
                CSPICE.gdpool("DELTET/EB", 0, 1)[0], m[0], m[1], deltaAT, leaps);
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
    static PositionResponse getRange(String observer, String target, String frame, String abcorr, boolean state, long start, long end, long deltat) {
        // Stopwatch sw = Stopwatch.createStarted();
        try {
            long dt = deltat * 1000;
            int len = (int) ((end - start) / dt) + 1;
            Position.Cartesian[] position = new Position.Cartesian[len];
            Position.Cartesian[] velocity = state ? new Position.Cartesian[len] : null;
            int i = 0;
            double[] v = new double[3];
            double[] sv = new double[6];
            for (long milli = start; milli <= end; milli += dt) {
                if (state) {
                    CSPICE.spkezr(target, milli2et(milli), frame, abcorr, observer, sv, lightTimeUnused);
                    v[0] = sv[0] * Sun.RadiusKMeterInv;
                    v[1] = sv[1] * Sun.RadiusKMeterInv;
                    v[2] = sv[2] * Sun.RadiusKMeterInv;
                    velocity[i] = new Position.Cartesian(milli, sv[3] * VelocityFactor, sv[4] * VelocityFactor, sv[5] * VelocityFactor);
                } else
                    position(observer, target, frame, abcorr, milli, v);
                position[i++] = new Position.Cartesian(milli, v[0], v[1], v[2]);
            }
            //System.out.println((sw.elapsed().toNanos() / 1e9));
            return new PositionResponse(position, velocity);
        } catch (Exception e) {
            Log.error(e);
        }
        return null;
    }

    @Nullable
    static Position getPositionLat(String observer, String target, String frame, JHVTime time) {
        try {
            double[] v = new double[3];
            position(observer, target, frame, "NONE", time.milli, v);
            v = SpiceMath.reclat(v);
            return new Position(time, v[0], v[1], v[2]);
        } catch (SpiceErrorException e) {
            Log.error(e);
        }
        return null;
    }

    @Nullable
    public static Position getCarrington(String target, JHVTime time) {
        try {
            double[] v = new double[3];
            position("SUN", target, "SOLO_IAU_SUN_2009", "NONE", time.milli, v);
            v = SpiceMath.recrad(v);
            return new Position(time, v[0], -v[1], v[2]);
        } catch (Exception e) {
            Log.error(e);
        }
        return null;
    }

    private static final int[] axes = {3, 2, 1};

    @Nullable
    public static double[] getRotationEuler(String fromFrame, String toFrame, JHVTime time) {
        try {
            double et = milli2et(time.milli);
            return CSPICE.m2eul(CSPICE.pxform(fromFrame, toFrame, et), axes);
        } catch (Exception e) {
            Log.error(e);
        }
        return null;
    }

    /// /

    // sec + CSPICE.deltet(sec, "UTC") as in deltet.c, without the native lock
    static double milli2et(long milli) {
        LeapSeconds ls = leapSeconds;
        double sec = (milli - TimeUtils.J2000.milli) / 1000.;
        int i = ls.leaps.length - 1;
        while (i > 0 && sec < ls.leaps[i])
            i--;
        double deltaAT = sec < ls.leaps[0] ? ls.deltaAT[0] - 1 : ls.deltaAT[i]; // as deltet before the first leap second
        double x = sec + ls.deltaTA + deltaAT;
        double m = ls.m0 + ls.m1 * Math.copySign(Math.floor(Math.abs(x) + .5), x); // d_nint, nearest ET second
        return sec + (deltaAT + ls.deltaTA + ls.k * Math.sin(m + ls.eb * Math.sin(m)));
    }

// --Commented out by Inspection START (19/06/2024, 16:04):
//    private static long et2milli(double et) throws SpiceErrorException {
//        double sec = et - CSPICE.deltet(et, "ET");
//        return (long) (sec * 1000. + TimeUtils.J2000.milli + .5);
//    }
// --Commented out by Inspection STOP (19/06/2024, 16:04)

    private static final double[] lightTimeUnused = new double[1];
    static final double VelocityFactor = 1000 / Sun.CLIGHT; // km/s to c

    private static void position(String observer, String target, String frame, String abcorr, long milli, double[] result) throws SpiceErrorException {
        double et = milli2et(milli);
        CSPICE.spkpos(target, et, frame, abcorr, observer, result, lightTimeUnused);
        result[0] *= Sun.RadiusKMeterInv;
        result[1] *= Sun.RadiusKMeterInv;
        result[2] *= Sun.RadiusKMeterInv;
    }

}
