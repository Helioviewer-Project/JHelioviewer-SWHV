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
    /** The step size for the filter */
    private Double stepSize;
    /** The units for this filter */
    private String units;

    /**
     * Creates a SWEK parameter filter of no type, with minimum value null,
     * maximum value null, startValue = null and stepSize = null.
     * 
     */
    public SWEKParameterFilter() {
        filterType = null;
        min = null;
        max = null;
        startValue = null;
        stepSize = null;
        units = "";
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
    public SWEKParameterFilter(String filterType, double min, double max, String units) {
        super();
        this.filterType = filterType;
        this.min = min;
        this.max = max;
        startValue = null;
        this.units = units;
    }

    /**
     * Creates a SWEK parameter filter description based on the given filter
     * type, the minimum value, the maximum value, the start value and null step
     * size.
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
    public SWEKParameterFilter(String filterType, double min, double max, double startValue, String units) {
        super();
        this.filterType = filterType;
        this.min = min;
        this.max = max;
        this.startValue = startValue;
        stepSize = null;
        this.units = units;
    }

    /**
     * Creates a SWEK parameter filter description based on the given filter
     * type, the minimum value, the maximum value, the start value and step
     * size.
     * 
     * @param filterType
     *            the filter
     * @param min
     *            the minimum value
     * @param max
     *            the maximum value
     * @param startValue
     *            the start value
     * @param stepSize
     *            the stepsize
     */
    public SWEKParameterFilter(String filterType, double min, double max, double startValue, Double stepsize, String units) {
        super();
        this.filterType = filterType;
        this.min = min;
        this.max = max;
        this.startValue = startValue;
        stepSize = stepsize;
        this.units = units;
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

    /**
     * Gets the step size of the filter.
     * 
     * @return the step size.
     */
    public Double getStepSize() {
        return stepSize;
    }

    /**
     * Sets the step size of the filter.
     * 
     * @param stepSize
     *            the step size
     */
    public void setStepSize(double stepSize) {
        this.stepSize = stepSize;
    }

    /**
     * Gets the units
     * 
     * @return the units
     */
    public String getUnits() {
        return units;
    }

    /**
     * Sets the units
     * 
     * @param units
     *            the new units
     */
    public void setUnits(String units) {
        this.units = units;
    }

}
