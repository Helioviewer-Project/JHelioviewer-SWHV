package org.helioviewer.jhv.data.event;

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
     * @param _filterType
     *            the filter
     * @param _min
     *            the minimum value
     * @param _max
     *            the maximum value
     * @param _startValue
     *            the start value
     * @param _dbType
     * @param _stepSize
     *            the stepsize
     */
    public SWEKParameterFilter(String _filterType, double _min, double _max, double _startValue, double _stepSize, String _units, String _dbType) {
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

    public String getFilterType() {
        return filterType;
    }

    public Double getMin() {
        return min;
    }

    public Double getMax() {
        return max;
    }

    public Double getStartValue() {
        return startValue;
    }

    public Double getStepSize() {
        return stepSize;
    }

    public String getUnits() {
        return units;
    }

}
