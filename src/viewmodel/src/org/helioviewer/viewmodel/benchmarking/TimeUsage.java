package org.helioviewer.viewmodel.benchmarking;

import org.helioviewer.base.logging.Log;

/**
 * A simple stopwatch class. It measures the time between the calls to start and
 * stop.
 * 
 * @author caplins
 * 
 */
public class TimeUsage {

    /** A simple driver method */
    public static void main(String[] args) {
        final int LOOP_COUNT = 32;
        final int BUFFER_LENGTH = 1024 * 256;

        TimeUsage timeUsage = new TimeUsage(LOOP_COUNT, TimeUsage.Precision.MICROSECONDS);
        timeUsage.start();
        for (int i = 0; i < LOOP_COUNT; i++) {
            int[] intBuf = new int[BUFFER_LENGTH];
            for (int j = 0; j < intBuf.length; j++)
                intBuf[j] = (int) System.nanoTime();
        }
        timeUsage.stop();
        timeUsage.print("Instantiate and fill an array of size " + BUFFER_LENGTH + " with System.nanoTime():");
    }

    /** An enum that specifies the time granularity to use. */
    public static enum Precision {
        SECONDS(1000000000, "seconds"), MILLISECONDS(1000000, "milliseconds"), MICROSECONDS(1000, "microseconds"), NANOSECONDS(1, "nanoseconds");
        /** The number */
        private int denom;
        private String unit;

        Precision(int _denom, String _unit) {
            denom = _denom;
            unit = _unit;
        }

        private String formatTimestamp(long _nanos) {
            return (String.valueOf(_nanos / denom) + " " + unit);
        }
    };

    private int numTests;
    private Precision prec;

    private long begin = 0L;
    private long end = 0L;

    public TimeUsage() {
        this(1);
    }

    public TimeUsage(int _numTests) {
        this(1, Precision.MILLISECONDS);
    }

    public TimeUsage(int _numTests, Precision _prec) {
        numTests = _numTests;
        prec = _prec;
    }

    public void start() {
        begin = System.nanoTime();
    }

    public void stop() {
        end = System.nanoTime();
    }

    public void print() {
        Log.info(prec.formatTimestamp((end - begin) / numTests));
    }

    public void print(Object _msg) {
        Log.info(_msg.toString() + " " + prec.formatTimestamp((end - begin) / numTests));
    }
};
