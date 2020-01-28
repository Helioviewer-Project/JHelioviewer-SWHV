package org.helioviewer.jhv.astronomy;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.threads.SingleExecutor;
import org.helioviewer.jhv.threads.JHVThread;
import org.helioviewer.jhv.time.JHVDate;

import spice.basic.Body;
import spice.basic.CSPICE;
import spice.basic.KernelDatabase;
import spice.basic.PositionVector;
import spice.basic.SpiceErrorException;
import spice.basic.SpiceException;
import spice.basic.TDBTime;

//import com.google.common.base.Stopwatch;

public class Spice extends Thread {

    private static final SingleExecutor executor = new SingleExecutor(new JHVThread.NamedClassThreadFactory(Spice.class, "SPICE"));

    public Spice(Runnable r, String name) {
        super(r, name);
    }

    public static void loadKernels(@Nonnull List<String> files) {
        executor.invokeLater(new LoadKernels(files));
    }

    private static class LoadKernels implements Runnable {

        private final List<String> files;

        LoadKernels(List<String> _files) {
            files = _files;
        }

        @Override
        public void run() {
            try {
                for (String f : files) {
                    KernelDatabase.load(Path.of(JHVGlobals.dataCacheDir, f).toString());
                }
            } catch (Exception e) {
                Log.error(e);
            }
        }

    }

    @Nullable
    public static String dateParse2UTC(@Nonnull String date) {
        try {
            return executor.invokeAndWait(new DateParse2UTC(date));
        } catch (Exception e) {
            Log.error(e);
        }
        return null;
    }

    private static class DateParse2UTC implements Callable<String> {

        private final String date;

        DateParse2UTC(String _date) {
            date = _date;
        }

        @Override
        public String call() throws SpiceErrorException {
            return new TDBTime(date).toUTCString("isoc", 0);
        }

    }

    @Nullable
    static PositionCartesian[] getPosition(@Nonnull Body observer, @Nonnull Body target, Frame frame, long start, long end, long deltat) {
        try {
            return executor.invokeAndWait(new GetPosition(observer, target, frame, start, end, deltat));
        } catch (Exception e) {
            Log.error(e);
        }
        return null;
    }

    private static class GetPosition implements Callable<PositionCartesian[]> {

        private final Body observer;
        private final Body target;
        private final Frame frame;
        private final long start;
        private final long end;
        private final long deltat;

        GetPosition(Body _observer, Body _target, Frame _frame, long _start, long _end, long _deltat) {
            observer = _observer;
            target = _target;
            frame = _frame;
            start = _start;
            end = _end;
            deltat = _deltat;
        }

        @Override
        public PositionCartesian[] call() throws SpiceException {
            // Stopwatch sw = Stopwatch.createStarted();
            long dt = deltat * 1000;
            PositionCartesian[] ret = new PositionCartesian[(int) ((end - start) / dt) + 1];
            int i = 0;
            for (long t = start; t <= end; t += dt) {
                ret[i++] = position(target, t, frame, observer);
            }
            //System.out.println((sw.elapsed().toNanos() / 1e9));
            return ret;
        }

    }

    private static PositionCartesian position(Body target, long milli, Frame frame, Body observer) throws SpiceException {
        TDBTime et = new TDBTime(milli2et(milli));
        PositionVector v = new PositionVector(target, et, frame.referenceFrame, AbCorrection.NONE.correction, observer);
        // System.out.println(">>> " + et.toUTCString("isoc", 0) + " " + new JHVDate(milli));
        return new PositionCartesian(milli,
                v.getElt(0) * Sun.RadiusKMeterInv,
                v.getElt(1) * Sun.RadiusKMeterInv,
                v.getElt(2) * Sun.RadiusKMeterInv);
    }

    @Nonnull
    public static Position getEarth(JHVDate time) {
        try {
            return executor.invokeAndWait(new GetEarth(time));
        } catch (Exception e) {
            Log.error(e);
        }
        return Sun.getEarth(time);
    }

    private static class GetEarth implements Callable<Position> {

        private final JHVDate time;

        GetEarth(JHVDate _time) {
            time = _time;
        }

        @Override
        public Position call() throws SpiceErrorException {
            double et = milli2et(time.milli);
            double[] lt = new double[1];
            double[] v = new double[3];
            CSPICE.spkpos("EARTH", et, "IAU_SUN", "NONE", "SUN", v, lt);
            double[] c = CSPICE.reclat(v);

            // like in Sun.getEarthInternal
            double lon = c[1];
            if (lon < 0)
                lon += 2 * Math.PI;
            return new Position(time, c[0] * Sun.RadiusKMeterInv, -lon, c[2]);
        }

    }

    private static final JHVDate J2000 = new JHVDate("2000-01-01T12:00:00");

    private static double milli2et(long milli) throws SpiceErrorException {
        double sec = (milli - J2000.milli) / 1000.;
        return sec + CSPICE.deltet(sec, "UTC");
    }

}
