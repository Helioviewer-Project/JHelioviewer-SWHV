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

        double scale = numPoints / (double) (stopMin - startMin);
        // System.out.println(">>> " + scale + " " + (stopMin - startMin + 1) + " " + numPoints);
        if (Math.abs(scale - 1) < 0.01) { // data already at ~1 min cadence
            // System.out.println(">>> " + Math.abs(scale - 1));
            return this;
        }

        int numBins = (int) (stopMin - startMin + 1);
        long[] datesBinned = new long[numBins];
        float[][] valuesBinned = new float[numAxes][numBins];

        if (scale < 1) { // upscaling
            for (int i = 0; i < numBins; i++) {
                datesBinned[i] = (startMin + i) * timeStep;
            }

            int numMiddles = numPoints - 1;
            long[] middles = new long[numMiddles];
            for (int i = 0; i < numMiddles; i++) {
                middles[i] = (dates[i + 1] + dates[i]) / 2;
            }

            for (int j = 0; j < numAxes; j++) {
                for (int i = 0; i < numBins; i++) {
                    int idx = -1 + (int) (i * scale + 0.5);
                    if (idx < 0) {
                        valuesBinned[j][i] = values[j][0];
                    } else if (idx > numMiddles - 1) {
                        valuesBinned[j][i] = values[j][numPoints - 1];
                    } else {
                        valuesBinned[j][i] = datesBinned[i] < middles[idx] ? values[j][idx] : values[j][idx + 1];
                    }
                }
            }

            return new DatesValues(datesBinned, valuesBinned);
        }

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
