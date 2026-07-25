package org.helioviewer.jhv.timelines.band;

import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.draw.YAxis;

record DatesValues(long[] dates, float[][] values) {

    DatesValues rebin() {
        int numPoints = dates.length;
        if (numPoints == 0)
            return this;

        long timeStep = TimeUtils.MINUTE_IN_MILLIS;
        long startMin = dates[0] / timeStep;
        long stopMin = dates[dates.length - 1] / timeStep;
        if (stopMin <= startMin)
            return this;

        double scale = numPoints / (double) (stopMin - startMin);
        if (Math.abs(scale - 1) < 0.1) // data already at ~1 min cadence
            return this;
        // System.out.println(">>> " + scale + " " + (stopMin - startMin + 1) + " " + numPoints);

        int numAxes = values.length;
        int numBins = (int) (stopMin - startMin + 1);
        float[][] valuesBinned = new float[numAxes][numBins];
        long[] datesBinned = new long[numBins];

        for (int i = 0; i < numBins; i++) {
            datesBinned[i] = (startMin + i) * timeStep;
        }

        if (scale < 1) { // upscaling
            int source = 0;
            for (int i = 0; i < numBins; i++) {
                while (source + 1 < numPoints
                        && datesBinned[i] >= dates[source] + (dates[source + 1] - dates[source]) / 2)
                    source++;
                for (int j = 0; j < numAxes; j++)
                    valuesBinned[j][i] = values[j][source];
            }
            return new DatesValues(datesBinned, valuesBinned);
        }

        int[] counts = new int[numBins];
        for (int j = 0; j < numAxes; j++) {
            float[] binned = valuesBinned[j];
            for (int i = 0; i < numPoints; i++) {
                float value = values[j][i];
                if (value != YAxis.BLANK) {
                    int idx = (int) (dates[i] / timeStep - startMin);
                    int n = ++counts[idx];
                    binned[idx] += (value - binned[idx]) / n;
                }
            }
            for (int i = 0; i < numBins; i++) {
                if (counts[i] == 0)
                    binned[i] = YAxis.BLANK;
                else
                    counts[i] = 0;
            }
        }
        return new DatesValues(datesBinned, valuesBinned);
    }

}
