package org.helioviewer.plugins.eveplugin.lines.data;


public class EVEValues {

    private final int index = 0;

    public final long[] dates;
    public final double[] minValues;
    public final double[] maxValues;

    private double minValue = Double.MAX_VALUE;
    private double maxValue = Double.MIN_VALUE;

    private final long intervalStart;
    private final long binStart;
    private final long binEnd;
    private final long timePerBin;
    private final int numOfBins;

    public EVEValues(long binStart, long binEnd, long intervalStart, int numOfBins, long timePerBin) {
        this.intervalStart = intervalStart;
        this.binStart = binStart;
        this.binEnd = binEnd;
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
        binEnd = 0;
        numOfBins = 0;
        timePerBin = 0;
        dates = new long[numOfBins];
        minValues = new double[numOfBins];
        maxValues = new double[numOfBins];
    }

    public void addValues(final long[] indates, final double[] invalues) {
        for (int i = 0; i < indates.length; i++) {
            double value = invalues[i];
            if (!Double.isNaN(value)) {
                int index = (int) ((indates[i] - binStart) / timePerBin);
                if (index >= 0 && index < numOfBins) {
                    maxValues[index] = Math.max(maxValues[index], value);
                    minValues[index] = Math.min(minValues[index], value);
                    minValue = value < minValue ? value : minValue;
                    maxValue = value > maxValue ? value : maxValue;
                } else {
                    // Log.debug("index out of bound avoided");
                    // Log.debug("indates : " + indates[i] + " | binStart : " +
                    // binStart + " | binEnd : " + binEnd +
                    // " | indates[i] - binStart : " + (indates[i] - binStart) +
                    // " | timePerBin : " + timePerBin + " | index : " + index +
                    // " | numOfBins : " + numOfBins);
                }

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
