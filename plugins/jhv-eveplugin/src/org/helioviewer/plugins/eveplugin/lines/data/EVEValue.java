package org.helioviewer.plugins.eveplugin.lines.data;

import java.util.Date;

/**
 * Each object of this class represents a pair of a time stamp and the
 * corresponding value to this time stamp.
 * 
 * @author Stephan Pagel
 * */
public class EVEValue {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    public Date date;
    public Double value;

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    /**
     * Default constructor.
     * */
    public EVEValue() {
        this(null, null);
    }

    /**
     * Parameterized constructor which initializes the object with the given
     * time stamp and value.
     * 
     * @param date
     *            time stamp
     * @param value
     *            corresponding value of the time stamp
     * */
    public EVEValue(final Date date, final Double value) {
        this.date = date;
        this.value = value;
    }

    /**
     * Returns the time stamp.
     * 
     * @return the time stamp
     * */
    public Date getDate() {
        return date;
    }

    /**
     * Returns the value.
     * 
     * @return the value
     * */
    public Double getValue() {
        return value;
    }

    /**
     * Sets the time stamp.
     * 
     * @param date
     *            new time stamp
     * */
    public void setDate(final Date date) {
        this.date = date;
    }

    /**
     * Sets the value.
     * 
     * @param value
     *            new value
     * */
    public void setValue(final Double value) {
        this.value = value;
    }
}
