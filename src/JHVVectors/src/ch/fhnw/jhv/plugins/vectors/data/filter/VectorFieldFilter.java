/**
 * 
 */
package ch.fhnw.jhv.plugins.vectors.data.filter;

import java.util.ArrayList;

import ch.fhnw.jhv.plugins.vectors.data.VectorData;
import ch.fhnw.jhv.plugins.vectors.data.VectorField;

/**
 * Abstract VectorField Filter class.
 * 
 * With this Filter class it is possible to create a filter chain for adapting
 * the given VectorField class.
 * 
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public abstract class VectorFieldFilter {

    /**
     * Contains the next Filter
     */
    private VectorFieldFilter nextFilter;

    /**
     * Constructor
     */
    public VectorFieldFilter() {

    }

    /**
     * Filter first the VectorField with the own filter As next step call the
     * method filterVectorField of the next setted filter
     * 
     * @param VectorField
     *            vectorField
     */
    public void filterVectorField(VectorField vectorField, int index, ArrayList<VectorData> filterResult) {

        // apply own filter method
        applyFilter(vectorField, index, filterResult);

        // apply filter on the next filter if it exists
        if (this.nextFilter != null)
            this.nextFilter.filterVectorField(vectorField, index, filterResult);

    }

    /**
     * Apply filter on the given VectorField
     * 
     * @param VectorField
     * 
     * @return VectorField
     */
    abstract void applyFilter(VectorField vectorField, int index, ArrayList<VectorData> target);

    /**
     * Set next filter in the filter chain
     * 
     * @param nextFilter
     *            next VectorFieldFilter
     */
    public void setNextFilter(VectorFieldFilter nextFilter) {
        this.nextFilter = nextFilter;
    }
}
