package org.helioviewer.jhv.astronomy;

import java.util.Locale;
import java.util.TimeZone;

import org.helioviewer.jhv.app.AppInit;
import org.helioviewer.jhv.app.Platform;
import org.helioviewer.jhv.io.Directories;
import org.helioviewer.jhv.time.TimeUtils;

import spice.basic.CSPICE;

// Spice.milli2et against CSPICE deltet, hourly 1960-2060 and around every leap second, within the 0.24 us resolution of ET in 2060
public class SpiceTest {

    public static void main(String[] arguments) throws Exception {
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Platform.init();
        Directories.createPersistentDirs();
        Directories.createCacheDirs();
        AppInit.loadSpice();

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
        System.out.println("PASS: UTC to ET against deltet, hourly 1960-2060 and around " + table.length / 2 + " leap seconds, worst " + worst + " s");
    }

    private static double difference(long milli) throws Exception {
        double sec = (milli - TimeUtils.J2000.milli) / 1000.;
        double et = sec + CSPICE.deltet(sec, "UTC");
        return Math.abs(Spice.milli2et(milli) - et);
    }

}
