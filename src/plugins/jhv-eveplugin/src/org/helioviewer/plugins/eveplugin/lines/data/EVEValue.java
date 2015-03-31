package org.helioviewer.plugins.eveplugin.lines.data;

import java.util.Date;

/**
 * Each object of this class represents a pair of a time stamp and the
 * corresponding value to this time stamp.
 * 
 * @author Stephan Pagel
 * */
public class EVEValue {

    public long milli;
    public double value;

    public EVEValue(long milli, double value) {
        this.milli = milli;
        this.value = value;
    }

}
