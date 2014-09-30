/**
 *
 */
package org.helioviewer.jhv.plugins.swek.config;

/**
 * Describes a SWEK parameter filter
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class SWEKParameterFilter {
    /** The type of filter */
    private String filterType;

    /** The minium value */
    private Double min;
    /** The maximum value */
    private Double max;
    /** The start value. */
    private Double startValue;

    /**
     * Creates a SWEK parameter filter of no type, with minimum value 0.0 and
     * maximum value 0.0.
     * 
     */
    public SWEKParameterFilter() {
        filterType = null;
        min = null;
        max = null;
        startValue = null;
    }

    /**
     * Creates a SWEK parameter filter description based on the given filter
     * type, the minimum value, the maximum value and null start value.
     * 
     * 
     * @param filterType
     *            The type of filter
     * @param min
     *            The minimum value
     * @param max
     *            The maximum value
     */
    public SWEKParameterFilter(String filterType, double min, double max) {
        super();
        this.filterType = filterType;
        this.min = min;
        this.max = max;
        startValue = null;
    }

    /**
     * Creates a SWEK parameter filter description based on the given filter
     * type, the minimum value, the maximum value and the start value.
     * 
     * @param filterType
     *            the filter
     * @param min
     *            the minimum value
     * @param max
     *            the maximum value
     * @param startValue
     *            the start value
     */
    public SWEKParameterFilter(String filterType, double min, double max, double startValue) {
        super();
        this.filterType = filterType;
        this.min = min;
        this.max = max;
        this.startValue = startValue;
    }

    /**
     * Gets the type of filter
     * 
     * @return the filterType
     */
    public String getFilterType() {
        return filterType;
    }

    /**
     * Sets the type of filter
     * 
     * @param filterType
     *            the filterType to set
     */
    public void setFilterType(String filterType) {
        this.filterType = filterType;
    }

    /**
     * Gets the minimum value of the parameter filter.
     * 
     * @return the min
     */
    public Double getMin() {
        return min;
    }

    /**
     * Sets the minimum filter of the parameter filter
     * 
     * @param min
     *            the min to set
     */
    public void setMin(double min) {
        this.min = min;
    }

    /**
     * Gets the maximum value of the parameter filter
     * 
     * @return the max
     */
    public Double getMax() {
        return max;
    }

    /**
     * Sets the maximum value of the parameter filter.
     * 
     * @param max
     *            the max to set
     */
    public void setMax(double max) {
        this.max = max;
    }

    /**
     * Gets the start value.
     * 
     * @return the start value
     */
    public Double getStartValue() {
        return startValue;
    }

    /**
     * Sets the start value.
     * 
     * @param startValue
     *            the start value
     */
    public void setStartValue(Double startValue) {
        this.startValue = startValue;
    }
}
