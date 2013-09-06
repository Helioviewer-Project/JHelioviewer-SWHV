package ch.fhnw.jhv.plugins.vectors.data.filter;

import java.util.ArrayList;

import ch.fhnw.jhv.plugins.vectors.data.VectorData;
import ch.fhnw.jhv.plugins.vectors.data.VectorField;

/**
 * Filter - Selection
 * 
 * Filters all vectors out which does not reach a defined range
 * <i>filterThresholdValue</i>.
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public class VectorFieldFilterSelection extends VectorFieldFilter {

    /**
     * Contains the threshold value
     */
    private double filterThresholdValue = 0;

    /**
     * Do the selection. Filter out short vectors. The manipulated vector field
     * is stored in the filterResult ArrayList.
     * 
     * @param vectorField
     *            VectorField
     * @param index
     *            time dimension
     * @param filterResult
     *            ArrayList<VectorData>
     */

    public void applyFilter(VectorField vectorField, int index, ArrayList<VectorData> filterResult) {

        for (int i = 0; i < filterResult.size(); i++) {
            if (filterResult.get(i) != null && filterResult.get(i).length < this.filterThresholdValue) {
                filterResult.set(i, null);
            }
        }
    }

    /**
     * @return the filterThresholdValue
     */
    public double getFilterThresholdValue() {
        return filterThresholdValue;
    }

    /**
     * @param filterThresholdValue
     *            the filterThresholdValue to set
     */
    public void setFilterThresholdValue(double filterThresholdValue) {
        this.filterThresholdValue = filterThresholdValue;
    }
}
