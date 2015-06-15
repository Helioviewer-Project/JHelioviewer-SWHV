package org.helioviewer.jhv.plugins.eveplugin.lines.data;

public class EVEValues {
    static int MINIMUMDISTANCEFORTIMEGAP = 60000;
    public final long[] dates;
    public final double[] minValues;
    public final double[] maxValues;

    private double minValue = Double.MAX_VALUE;
    private double maxValue = Double.MIN_VALUE;

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
        minValues = new double[numOfBins];
        maxValues = new double[numOfBins];
        fillArrays();
    }

    public EVEValues() {
        intervalStart = -1;
        binStart = 0;
        numOfBins = 0;
        timePerBin = 0;
        dates = new long[numOfBins];
        minValues = new double[numOfBins];
        maxValues = new double[numOfBins];
    }

    public void addValues(final long[] indates, final double[] invalues) {
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
                maxValues[i] = Math.max(maxValues[i], value);
                minValues[i] = Math.min(minValues[i], value);
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
            maxValues[0] = Double.MIN_VALUE;
            minValues[0] = Double.MAX_VALUE;
            for (int i = 1; i < numOfBins; i++) {
                dates[i] = dates[i - 1] + timePerBin;
                maxValues[i] = Double.MIN_VALUE;
                minValues[i] = Double.MAX_VALUE;
            }
        }
    }
}
