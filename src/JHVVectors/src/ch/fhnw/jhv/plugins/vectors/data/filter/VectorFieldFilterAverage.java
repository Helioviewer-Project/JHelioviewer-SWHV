package ch.fhnw.jhv.plugins.vectors.data.filter;

import java.util.ArrayList;

import ch.fhnw.jhv.plugins.PluginManager;
import ch.fhnw.jhv.plugins.interfaces.ControlPlugin.ControlPluginType;
import ch.fhnw.jhv.plugins.vectors.control.SettingsControlPlugin;
import ch.fhnw.jhv.plugins.vectors.data.VectorData;
import ch.fhnw.jhv.plugins.vectors.data.VectorField;
import ch.fhnw.jhv.plugins.vectors.data.VectorFieldManager;

/**
 * Filter - Average
 * 
 * Combines a defined amount of vectores together using an averaging method.
 * 
 * Example:
 * 
 * The values are the length of a vector. In real we do this averaging also with
 * the azimut and inclination angle.
 * 
 * Averaging factor is <b>2</b>
 * 
 * _ _ _ _ _ _ _ _ | 4 | 5 | | | |_ _|_ _ | ==> |4.25 | | 6 | 2 | |_ _ _| |_ _ _
 * _ _
 * 
 * 
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public class VectorFieldFilterAverage extends VectorFieldFilter {

    /**
     * Max amount of vectors which should be rendered. Otherwise the
     * visualization could be too slow.
     */
    private int maxAmountOfVectors = 40000;

    /**
     * Current averaging factor
     */
    private int averageFactor = 2;

    /**
     * Old averaging factor
     */
    private int oldAverageFactor = 0;

    /**
     * Is it the first time called
     */
    private boolean calculateAverageFactor = true;

    /**
     * Do the averaging and save the newly calculated vector field in the
     * filterResult ArrayList.
     * 
     * @param vectorField
     *            VectorField
     * @param index
     *            time dimension
     * @param filterResult
     *            ArrayList<VectorData>
     */

    public void applyFilter(VectorField vectorField, int index, ArrayList<VectorData> filterResult) {

        // Copy all the values into the list
        if (oldAverageFactor != averageFactor && averageFactor == 1) {

            // clear list
            filterResult.clear();

            for (VectorData vector : vectorField.vectors[index]) {
                filterResult.add(vector.clone());
            }

            this.oldAverageFactor = averageFactor;
        } else if (oldAverageFactor != averageFactor || VectorFieldManager.getInstance().getShouldUseOriginal()) {

            // first load
            if (calculateAverageFactor) {

                // calculate optimal averaging start value to visualize not more
                // than maxAmountOfVectors
                averageFactor = determineAverageStartValue(vectorField, index);

                // update the gui with the new values
                ((SettingsControlPlugin) PluginManager.getInstance().getControlPluginByType(ControlPluginType.SETTINGS)).setAverageFactor(averageFactor);

                // System.out.println("Did determine perfect average value to: "
                // + averageFactor);

                calculateAverageFactor = false;
            }

            // the size of the new generated vectorfield
            int origFieldSize = (int) (vectorField.sizePixel.x * vectorField.sizePixel.y);
            int size = origFieldSize / (averageFactor * averageFactor) + (int) (vectorField.sizePixel.x % averageFactor) * (int) vectorField.sizePixel.y + (int) (vectorField.sizePixel.y % averageFactor) * (int) vectorField.sizePixel.x;

            filterResult.ensureCapacity(size);

            for (int y = 0; y < vectorField.sizePixel.y; y += averageFactor) {
                for (int x = 0; x < vectorField.sizePixel.x; x += averageFactor) {

                    VectorData[] vectorDataStart = new VectorData[averageFactor * averageFactor];

                    // used as index for the new VectorData vectors array
                    int counter = 0;

                    int i = 0;
                    int j = 0;

                    // GET ALL VALUES (X/Y)
                    // -----------------------
                    for (j = y; (j < averageFactor + y) && (j < vectorField.sizePixel.y); j++) {
                        for (i = x; (i < averageFactor + x) && (i < vectorField.sizePixel.x); i++) {
                            vectorDataStart[counter] = vectorField.vectors[index][j * (int) vectorField.sizePixel.x + i];

                            counter++;
                        }
                    }

                    // Do the averaging
                    VectorData newVectorDataStart = calculateAverageVector(vectorDataStart);

                    // TODO: lower right point.. should be in the center?!
                    newVectorDataStart.x = i - (i - x) / 2;
                    newVectorDataStart.y = j - (j - y) / 2;

                    filterResult.add(newVectorDataStart);
                }
            }
            this.oldAverageFactor = averageFactor;
        }
    }

    /**
     * Calculate the average azimut, inclination angle and the average length
     * 
     * @param vectorData
     *            array of VectorData
     * 
     * @return VectorData
     */
    private VectorData calculateAverageVector(VectorData[] vectorData) {

        VectorData newVectorData = new VectorData();

        float sumLength = 0;

        // Get the sum of all vectors
        for (VectorData v : vectorData) {
            if (v != null) {
                sumLength += v.length;
            }
        }

        float resultAzimut = 0;
        float resultInclination = 0;

        for (VectorData v : vectorData) {
            if (v != null) {
                // A longer vector has more weight on the calculation
                resultAzimut += v.azimuth * v.length;
                resultInclination += v.inclination * v.length;
            }
        }

        // calculate new values (simple averaging) -> sum / amount of vectors
        newVectorData.azimuth = resultAzimut / sumLength;
        newVectorData.inclination = resultInclination / sumLength;
        newVectorData.length = sumLength / vectorData.length;

        return newVectorData;
    }

    /**
     * Determine optimal averaging factor
     * 
     * @param vf
     *            VectorField
     * @param index
     *            time dimension
     * 
     * @return int optimal averaging value
     */
    private int determineAverageStartValue(VectorField vf, int index) {

        int size = vf.vectors[index].length;

        if (size < maxAmountOfVectors) {
            return 1; // no averaging necessary
        } else {
            return (int) Math.ceil(Math.sqrt((double) size / maxAmountOfVectors));
        }
    }

    /**
     * @return the averageFactor
     */
    public int getAverageFactor() {
        return averageFactor;
    }

    /**
     * @param averageFactor
     *            the averageFactor to set
     */
    public void setAverageFactor(int averageFactor) {
        this.averageFactor = averageFactor;
    }

    /**
     * Set first time value
     * 
     * @param boolean firstTime
     */
    public void setCalculateAverageFactor(boolean calculateAverageFactor) {
        this.calculateAverageFactor = calculateAverageFactor;
    }
}
