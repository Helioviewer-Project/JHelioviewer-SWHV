package org.helioviewer.jhv.events;

public class SWEKParameterFilter {

    private final String filterType;
    private final Double min;
    private final Double max;
    private final Double startValue;
    private final Double stepSize;
    private final String units;
    private final String dbType;
    
    // Creates a SWEK parameter filter description based on the given filter
    // type, the minimum value, the maximum value, the start value and step size
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
