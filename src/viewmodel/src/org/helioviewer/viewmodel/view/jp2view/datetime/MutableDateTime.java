package org.helioviewer.viewmodel.view.jp2view.datetime;

/**
 * Class is a mutable version of the ImmutableDateTime class.
 * 
 * @author caplins
 * 
 */
public class MutableDateTime extends ImmutableDateTime {

    /**
     * Calls inherited constructor.
     * 
     * @param _year
     * @param _month
     * @param _day
     * @param _hour
     * @param _minute
     * @param _second
     */
    public MutableDateTime(int _year, int _month, int _day, int _hour, int _minute, int _second) {
        super(_year, _month, _day, _hour, _minute, _second);
    }

    /**
     * A convenience method to assign the time from another object to this one.
     * 
     * @param _dt
     */
    public void setDateTime(ImmutableDateTime _dt) {
        this.calendar.setTime(_dt.calendar.getTime());
    }

    /**
     * A method to set the date fields of the Calendar object used by this
     * class. The field keys are the same as the Calendar class.
     * 
     * @param _field
     * @param _value
     */
    public void setField(int _field, int _value) {
        this.calendar.set(_field, _value);
    }

    /**
     * A method to change the date fields of the Calendar object used by this
     * class. The field keys are the same as the Calendar class.
     * 
     * @param _field
     * @param _delta
     */
    public void addToField(int _field, int _delta) {
        this.calendar.add(_field, _delta);
    }
};
