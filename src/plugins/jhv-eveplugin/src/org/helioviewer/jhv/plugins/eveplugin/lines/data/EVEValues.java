package org.helioviewer.jhv.plugins.eveplugin.lines.data;

public class EVEValues {

    private static final long MINIMUMDISTANCEFORTIMEGAP = 60000;

    public final long[] dates;
    public final float[] maxValues;

    private float minValue = Float.MAX_VALUE;
    private float maxValue = Float.MIN_VALUE;

    private final long intervalStart;
    private final long binStart;
    private final long timePerBin;
    private final int numOfBins;

    public EVEValues(long binStart, long binEnd, long intervalStart, int numOfBins, long timePerBin) {
        this.intervalStart = intervalStart;
        this.binStart = binStart;
        this.timePerBin = timePerBin;
        this.numOfBins = numOfBins;
        dates = new long[numOfBins];
        maxValues = new float[numOfBins];
        fillArrays();
    }

    public EVEValues() {
        intervalStart = -1;
        binStart = 0;
        numOfBins = 0;
        timePerBin = 0;
        dates = new long[0];
        maxValues = new float[0];
    }

    public void addValues(final long[] indates, final float[] invalues) {
        int j = 0;
        long tg = Math.max(MINIMUMDISTANCEFORTIMEGAP, timePerBin / 2);
        for (int i = 0; i < maxValues.length; i++) {
            long startt = binStart + (timePerBin * i) + timePerBin / 2;

            while (j >= 1 && j < indates.length && ((indates[j] - startt) >= -tg)) {
                j--;
            }

            while (j < indates.length && (indates[j] - startt) <= -tg) {
                j++;
            }

            while (j < indates.length && indates[j] - startt <= tg) {
                float value = invalues[j];
                if (!Float.isNaN(value)) {
                    maxValues[i] = Math.max(maxValues[i], value);
                    minValue = Math.min(value, minValue);
                }
                j++;
            }
            maxValue = Math.max(maxValues[i], maxValue);
        }
    }

    public int getNumberOfValues() {
        return numOfBins;
    }

    public float getMinimumValue() {
        return minValue;
    }

    public float getMaximumValue() {
        return maxValue;
    }

    private void fillArrays() {
        if (numOfBins > 0) {
            dates[0] = intervalStart;
            maxValues[0] = Float.MIN_VALUE;
            for (int i = 1; i < numOfBins; i++) {
                dates[i] = dates[i - 1] + timePerBin;
                maxValues[i] = Float.MIN_VALUE;
            }
        }
    }

}
