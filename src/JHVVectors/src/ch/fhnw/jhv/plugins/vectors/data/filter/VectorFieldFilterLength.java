/**
 * 
 */
package ch.fhnw.jhv.plugins.vectors.data.filter;

import java.util.ArrayList;

import ch.fhnw.jhv.plugins.vectors.data.VectorData;
import ch.fhnw.jhv.plugins.vectors.data.VectorField;
import ch.fhnw.jhv.plugins.vectors.data.VectorFieldManager;

/**
 * Filter - Length
 * 
 * With this Filter it is possible to scale the vectors length.
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public class VectorFieldFilterLength extends VectorFieldFilter {

    /**
     * Current length scale value
     */
    private float lengthScaleValue = 0.0015f;

    /**
     * Old length scale value
     */
    private float lengthScaleOldValue = 0;

    /**
     * Scale the vectors and return the newly generated ArrayList as
     * filterResults.
     * 
     * @param vectorField
     *            VectorField
     * @param index
     *            time dimension
     * @param filterResult
     *            ArrayList<VectorData>
     */

    public void applyFilter(VectorField vectorField, int index, ArrayList<VectorData> filterResult) {

        if (lengthScaleOldValue != lengthScaleValue || VectorFieldManager.getInstance().getShouldUseOriginal()) {
            for (VectorData vector : filterResult) {
                if (vector != null) {
                    if (this.lengthScaleOldValue == 0 || VectorFieldManager.getInstance().getShouldUseOriginal()) {

                        // the old value isn't set currently so just ignore it
                        // and work only with the new value
                        vector.length = vector.length * this.lengthScaleValue;
                    } else {

                        // scale back with the old scale and then do the new
                        // scale
                        vector.length = vector.length * (1.0f / this.lengthScaleOldValue) * this.lengthScaleValue;
                    }
                }
            }

            lengthScaleOldValue = lengthScaleValue;
        }

    }

    /**
     * @return the lengthScaleOldValue
     */
    public double getLengthScaleOldValue() {
        return lengthScaleOldValue;
    }

    /**
     * @param lengthScaleOldValue
     *            the lengthScaleOldValue to set
     */
    public void setLengthScaleOldValue(float lengthScaleOldValue) {
        this.lengthScaleOldValue = lengthScaleOldValue;
    }

    /**
     * @return the lengthScaleValue
     */
    public float getLengthScaleValue() {
        return lengthScaleValue;
    }

    /**
     * @param lengthScaleValue
     *            the lengthScaleValue to set
     */
    public void setLengthScaleValue(float lengthScaleValue) {
        this.lengthScaleValue = lengthScaleValue;
    }
}
