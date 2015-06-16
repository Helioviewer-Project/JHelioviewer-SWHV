package org.helioviewer.jhv.plugins.eveplugin.lines.data;

public class EVEValues {
    static int MINIMUMDISTANCEFORTIMEGAP = 60000;
    public final long[] dates;
    public final float[] maxValues;

    private double minValue = Float.MAX_VALUE;
    private double maxValue = Float.MIN_VALUE;

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
            while (j < indates.length && indates[j] - startt <= tg && !Double.isNaN(invalues[j])) {
                double value = invalues[j];
                maxValues[i] = (float) Math.max(maxValues[i], value);
                minValue = value < minValue ? value : minValue;
                maxValue = value > maxValue ? value : maxValue;
                j++;
            }

        }
    }

    public int getNumberOfValues() {
        return numOfBins;
    }

    public double getMinimumValue() {
        return minValue;
    }

    public double getMaximumValue() {
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
