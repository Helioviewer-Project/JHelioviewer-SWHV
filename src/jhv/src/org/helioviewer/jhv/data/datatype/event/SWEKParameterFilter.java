package org.helioviewer.jhv.data.datatype.event;

// Describes a SWEK parameter filter
public class SWEKParameterFilter {

    /** The type of filter */
    private final String filterType;

    /** The minium value */
    private final Double min;
    /** The maximum value */
    private final Double max;
    /** The start value. */
    private final Double startValue;
    /** The step size for the filter */
    private final Double stepSize;
    /** The units for this filter */
    private final String units;
    private final String dbType;

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
     * @param dbtype
     * @param stepSize
     *            the stepsize
     */
    public SWEKParameterFilter(String _filterType, double _min, double _max, double _startValue, Double _stepSize, String _units, String _dbType) {
        filterType = _filterType;
        min = _min;
        max = _max;
        startValue = _startValue;
        stepSize = _stepSize;
        units = _units;
        dbType = _dbType;
    }

    public String getDbType() {
        return dbType;
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
     * Gets the minimum value of the parameter filter.
     *
     * @return the min
     */
    public Double getMin() {
        return min;
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
     * Gets the start value.
     *
     * @return the start value
     */
    public Double getStartValue() {
        return startValue;
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
     * Gets the units
     *
     * @return the units
     */
    public String getUnits() {
        return units;
    }

}
