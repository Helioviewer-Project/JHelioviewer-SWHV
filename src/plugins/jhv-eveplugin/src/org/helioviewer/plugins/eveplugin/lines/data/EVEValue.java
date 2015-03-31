package org.helioviewer.plugins.eveplugin.lines.data;

import java.util.Date;

/**
 * Each object of this class represents a pair of a time stamp and the
 * corresponding value to this time stamp.
 * 
 * @author Stephan Pagel
 * */
public class EVEValue {

    public Date date;
    public double value;

    public EVEValue(Date date, double value) {
        this.date = date;
        this.value = value;
    }

}
