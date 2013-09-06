package org.helioviewer.plugins.eveplugin.base;

/**
 * @author Stephan Pagel
 * */
public class Range {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////
    
    public double min = Double.MAX_VALUE;
    public double max = Double.MIN_VALUE;
    
    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////
    
    public Range() {
        this(Double.MAX_VALUE, Double.MIN_VALUE);
    }
    
    public Range(final double min, final double max) {
        this.min = min;
        this.max = max;
    }
    
    public Range(final Range other) {
        this.min = other.min;
        this.max = other.max;
    }
    
    public void reset() {
        min = Double.MAX_VALUE;
        max = Double.MIN_VALUE;    
    }
    
    public boolean setMin(final double min) {
        if (this.min > min) {
            this.min = min;
            return true;
        }
        
        return false;
    }
    
    public boolean setMax(final double max) {
        if (this.max < max) {
            this.max = max;
            return true;
        }
        
        return false;
    }
    
    public void setMinMax(final double value) {
        min = min < value ? min : value;
        max = max > value ? max : value;
    }
    
    public void combineWith(final Range range) {
        min = min < range.min ? min : range.min;
        max = max < range.max ? max : range.max;
    }
    
    public boolean isPositivRange() {
        return min <= max;
    }
    
    public boolean contains(final Range range) {
        return range.min >= this.min && range.max <= this.max;
    }
}
