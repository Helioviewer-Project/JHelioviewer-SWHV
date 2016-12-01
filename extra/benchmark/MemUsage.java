package org.helioviewer.viewmodel.benchmarking;

import org.helioviewer.base.logging.Log;

/**
 * A rudimentary class used for measuring a change in memory usage.
 * 
 * @author caplins
 * 
 */
public class MemUsage {

    /** An enum designating the units to use in the output */
    public static enum Precision {
        BYTES(1, "bytes"), KILOBYTES(1 << 10, "kilobytes"), MEGABYTES(1 << 20, "megabytes"), GIGABYTES(1 << 30, "gigabytes");
        private int denom;
        private String unit;

        Precision(int _denom, String _unit) {
            denom = _denom;
            unit = _unit;
        }

        private String formatMemory(long _bytes) {
            return (String.valueOf(_bytes / denom) + " " + unit);
        }
    };

    /**
     * How long the class should sleep after a GC and before querying the memory
     * usage
     */
    private static final int SLEEP_TIME = 250;

    /** The runtime */
    private static final Runtime runtime = Runtime.getRuntime();

    /** The designated precision */
    private Precision prec;

    /** The memory usage at the beginning */
    private long begin = 0L;

    /** The memory usage at the end */
    private long end = 0L;

    /** Default constructor, gives byte precision */
    public MemUsage() {
        this(MemUsage.Precision.BYTES);
    }

    /** Constructor, gives precision based on argument */
    public MemUsage(Precision _prec) {
        prec = _prec;
    }

    /** Determines the memory usage at the beginning of the test period. */
    public void start() {
        attemptCleanup();
        begin = getCurrentMemUsage();
    }

    /** Determines the memory usage at the end of the test period. */
    public void stop() {
        attemptCleanup();
        end = getCurrentMemUsage();
    }

    /** Calls the GC and allows it some time to work its magic */
    public void attemptCleanup() {
        runtime.gc();
        try {
            Thread.sleep(SLEEP_TIME);
        } catch (Exception ex) {
        }
    }

    /** Gets the current absolute memory usage in bytes */
    public static long getCurrentMemUsage() {
        return (runtime.totalMemory() - runtime.freeMemory());
    }

    /** Prints the memory usage to sysout */
    public void print() {
        Log.info(prec.formatMemory(end - begin));
    }

    /** Prints the memory usage to sysout with the specified message */
    public void print(Object _obj) {
        Log.info(_obj.toString() + " " + prec.formatMemory(end - begin));
    }
};