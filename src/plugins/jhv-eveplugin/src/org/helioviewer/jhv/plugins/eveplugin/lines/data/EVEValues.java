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
        /*
         * boolean[] added = new boolean[indates.length];
         * 
         * for (int i = 0; i < added.length; i++) { added[i] = false; }
         */
        int j = 0;
        long tg = Math.max(MINIMUMDISTANCEFORTIMEGAP, timePerBin / 2);
        for (int i = 0; i < maxValues.length; i++) {
            long startt = binStart + (timePerBin * i) + timePerBin / 2;

            /*
             * Log.debug("bin : " + i + " starttime " + startt + " tg " + tg);
             * Log.debug("j at while " + j); if (j < indates.length) {
             * Log.debug(" indates " + indates[j] + " " + invalues[j]); }
             */
            while (j >= 1 && j < indates.length && ((indates[j] - startt) >= -tg)) {
                j--;
                // Log.debug("j in first while " + j + " indates " + indates[j]
                // + " " + invalues[j] + " condition " + ((indates[j] - startt)
                // >= -tg));
            }
            // Log.debug("j after first while " + j);
            while (j < indates.length && (indates[j] - startt) <= -tg) {
                j++;

                /*
                 * if (j < indates.length) { Log.debug("j in second while " + j
                 * + " indates " + indates[j] + " " + invalues[j] +
                 * " condition " + ((indates[j] - startt) <= -tg)); }
                 */

            }
            // Log.debug("j after second while " + j);
            while (j < indates.length && indates[j] - startt <= tg) {
                double value = invalues[j];
                // added[j] = true;
                if (!Double.isNaN(invalues[j])) {
                    maxValues[i] = (float) Math.max(maxValues[i], value);
                    minValue = value < minValue ? value : minValue;
                    maxValue = value > maxValue ? value : maxValue;
                }
                j++;

                /*
                 * Log.debug("j in last while " + j); if (j < indates.length) {
                 * Log.debug((j < indates.length) + " " + (indates[j] - startt
                 * <= tg) + " " + !Double.isNaN(invalues[j])); }
                 */

            }
            // Log.debug("j after last while " + j + " i " + i);
        }

        /*
         * Log.debug("number indates" + added.length);
         * Log.debug("number maxValues " + maxValues.length); for (int i = 0; i
         * < added.length; i++) { if (added[i] == false &&
         * !Double.isNaN(invalues[i])) { if (indates[i] >= binStart &&
         * indates[i] <= dates[dates.length - 1] + timePerBin / 2) { Log.debug(i
         * + " " + indates[i] + " not added interval start = " + binStart +
         * " intervalEnd " + (dates[dates.length - 1] + timePerBin / 2) + " ");
         * for (int k = 0; k < maxValues.length; k++) { long startt = binStart +
         * (timePerBin * k) + timePerBin / 2;
         * 
         * if (indates[i] >= binStart + (timePerBin * k) && indates[i] <=
         * binStart + (timePerBin * (k + 1))) { Log.debug(startt + " " + tg);
         * Log.debug(i >= 1 && j < indates.length && ((indates[i] - startt) >=
         * -tg)); Log.debug(i < indates.length && (indates[i] - startt) <= -tg);
         * Log.debug(i < indates.length && indates[i] - startt <= tg &&
         * !Double.isNaN(invalues[i])); } else {
         * 
         * } } } } }
         */
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
