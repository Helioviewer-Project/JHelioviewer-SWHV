package org.helioviewer.viewmodel.view.jp2view.datetime;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Class that holds Date and Time information. The data is stored in a Calendar
 * object. This class is immutable, if you want a mutable version look at
 * MutableDateTime.
 *
 * @author caplins
 *
 */
public class ImmutableDateTime implements Comparable<ImmutableDateTime> {

    /** Default DateFormat used to format the date. */
    protected static final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);

    /** Default DateFormat used to format the time. */
    protected static final DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM);

    /** Internal class that holds date/time information. */
    protected Calendar calendar;
    protected String cachedDate;

    /**
     * The constructor that populates the fields of the internal Calendar
     * object. No arguments may be negative or an exception will be thrown.
     * NOTE: Month argument is zero based... i.e. January corresponds to 0
     */
    public ImmutableDateTime(int _year, int _month, int _day, int _hour, int _minute, int _second) {
        if ((_year | _month | _day | _hour | _minute | _second) < 0)
            throw new IllegalArgumentException("Arguments cannot be negative!");
        try {
            calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+00:00"));
            calendar.clear();
            calendar.set(_year, _month, _day, _hour, _minute, _second);
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+0000"));
            timeFormat.setTimeZone(TimeZone.getTimeZone("GMT+0000"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ImmutableDateTime(long seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("Arguments cannot be negative!");
        }

        try {
            calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+00:00"));
            calendar.clear();
            calendar.setTimeInMillis(seconds * 1000);
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+0000"));
            timeFormat.setTimeZone(TimeZone.getTimeZone("GMT+0000"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ImmutableDateTime(ImmutableDateTime original) {
        if (original == null) {
            throw new IllegalArgumentException("Can not copy null object");
        }

        try {
            calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+00:00"));
            calendar.clear();
            calendar.setTimeInMillis(original.getMillis());
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+0000"));
            timeFormat.setTimeZone(TimeZone.getTimeZone("GMT+0000"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the requested field. Field keys are the same as the Calendar
     * class, since that is the underlying DateTime representation. NOTE: The
     * month is zero based... i.e. January is represented by 0
     *
     * @param _field
     * @return Requested field
     */
    public int getField(int _field) {
        return calendar.get(_field);
    }

    /** Returns the number of milliseconds since the Epoch. */
    public long getMillis() {
        return calendar.getTimeInMillis();
    }

    /** Returns the internal Date formatted to a String appropriately. */
    public String getFormattedDate() {
        return dateFormat.format(calendar.getTime());
    }

    /** Returns the internal Time formatted to a String appropriately. */
    public String getFormattedTime() {
        return timeFormat.format(calendar.getTime());
    }

    public Date getTime() {
        return calendar.getTime();
    }

    /**
     * Via the Comparable interface. This method will be used to sort the
     * DataTime objects.
     */
    @Override
    public int compareTo(ImmutableDateTime _dt) {
        long diff = (calendar.getTimeInMillis() - _dt.calendar.getTimeInMillis());
        return diff < 0 ? -1 : (diff > 0 ? +1 : 0);
    }

    /** Overridden equals method */
    public boolean equals(ImmutableDateTime _dt) {

        if (_dt == null)
            return false;

        return compareTo(_dt) == 0;
    }

    /** Overridden hashCode method */

    @Override
    public int hashCode() {
        return (int) (calendar.getTimeInMillis() ^ (calendar.getTimeInMillis() >>> 32));
    }

    public String getCachedDate() {
        if (this.cachedDate == null) {
            SimpleDateFormat dateFormat;
            dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            this.cachedDate = dateFormat.format(calendar.getTime());
        }
        return this.cachedDate;

    }
};
