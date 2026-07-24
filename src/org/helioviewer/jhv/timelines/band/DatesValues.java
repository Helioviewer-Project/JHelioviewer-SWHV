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
