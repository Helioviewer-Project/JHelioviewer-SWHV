package org.helioviewer.jhv.timelines.band;

import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.draw.YAxis;

public record DatesValues(long[] dates, float[][] values) {

    private static class Bin {

        private int n = 0;
        private float mean = 0;

        void add(float val) {
            if (val != YAxis.BLANK) {
                n++;
                mean += (val - mean) / n;
            }
        }

        float getMean() {
            return n == 0 ? YAxis.BLANK : mean;
        }

    }

    DatesValues rebin() {
        int numAxes = values.length;
        int numPoints = values[0].length;
        if (numPoints == 0)
            return this;

        long rebinFactor = TimeUtils.MINUTE_IN_MILLIS;
        long startBin = dates[0] / rebinFactor;
        long endBin = dates[dates.length - 1] / rebinFactor;
        int numBins = (int) (endBin - startBin + 1);

        Bin[][] bins = new Bin[numAxes][numBins];
        for (int j = 0; j < numAxes; j++) {
            for (int i = 0; i < numBins; i++) {
                bins[j][i] = new Bin();
            }
        }
        for (int j = 0; j < numAxes; j++) {
            for (int i = 0; i < numPoints; i++) {
                bins[j][(int) (dates[i] / rebinFactor - startBin)].add(values[j][i]);
            }
        }

        long[] datesBinned = new long[numBins];
        for (int i = 0; i < numBins; i++) {
            datesBinned[i] = (startBin + i) * rebinFactor;
        }
        float[][] valuesBinned = new float[numAxes][numBins];
        for (int j = 0; j < numAxes; j++) {
            for (int i = 0; i < numBins; i++) {
                valuesBinned[j][i] = bins[j][i].getMean();
            }
        }

        return new DatesValues(datesBinned, valuesBinned);
    }

}
