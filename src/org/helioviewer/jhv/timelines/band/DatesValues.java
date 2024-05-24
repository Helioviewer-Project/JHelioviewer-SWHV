package org.helioviewer.jhv.timelines.band;

import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.draw.YAxis;

record DatesValues(long[] dates, float[][] values) {

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

        long timeStep = TimeUtils.MINUTE_IN_MILLIS;
        long startMin = dates[0] / timeStep;
        long stopMin = dates[dates.length - 1] / timeStep;

        double scale = (stopMin - startMin) / (double) numPoints;
        // System.out.println(">>> " + scale + " " + (stopMin - startMin + 1) + " " + numPoints);
        if (Math.abs(scale - 1) < 0.01) { // data already at ~1 min cadence
            // System.out.println(">>> " + Math.abs(scale - 1));
            return this;
        }

        if (scale > 1) { // upscaling
            int numBins = 1 + (int) (stopMin - startMin + scale + 0.5);
            long[] datesBinned = new long[numBins];
            float[][] valuesBinned = new float[numAxes][numBins];

            long date = (long) (dates[0] - 0.5 * (scale * timeStep) + 0.5);
            for (int i = 0; i < numBins; i++) {
                datesBinned[i] = date;
                date += timeStep;
            }

            /*
            long startDate = (long) (dates[0] - 0.5 * (scale * timeStep) + 0.5);
            long stopDate  = (long) (dates[numPoints - 1] + 0.5 * (scale * timeStep) + 0.5);
            System.out.println(">>> " + TimeUtils.format(dates[0]) + " " + TimeUtils.format(dates[numPoints - 1]));
            System.out.println(">>> " + TimeUtils.format(startDate) + " " + TimeUtils.format(stopDate));
            System.out.println(">>> " + TimeUtils.format(datesBinned[0]) + " " + TimeUtils.format(datesBinned[numBins - 1]));
            */

            for (int j = 0; j < numAxes; j++) {
                for (int i = 0; i < numBins; i++) {
                    int idx = -1 + (int) (i / scale);
                    valuesBinned[j][i] = values[j][MathUtils.clip(idx, 0, numPoints - 1)];
                }
            }

            return new DatesValues(datesBinned, valuesBinned);
        }

        int numBins = (int) (stopMin - startMin + 1);
        long[] datesBinned = new long[numBins];
        float[][] valuesBinned = new float[numAxes][numBins];

        Bin[][] bins = new Bin[numAxes][numBins];
        for (int j = 0; j < numAxes; j++) {
            for (int i = 0; i < numBins; i++) {
                bins[j][i] = new Bin();
            }
        }
        for (int j = 0; j < numAxes; j++) {
            for (int i = 0; i < numPoints; i++) {
                bins[j][(int) (dates[i] / timeStep - startMin)].add(values[j][i]);
            }
        }

        for (int i = 0; i < numBins; i++) {
            datesBinned[i] = (startMin + i) * timeStep;
        }
        for (int j = 0; j < numAxes; j++) {
            for (int i = 0; i < numBins; i++) {
                valuesBinned[j][i] = bins[j][i].getMean();
            }
        }

        return new DatesValues(datesBinned, valuesBinned);
    }

}
