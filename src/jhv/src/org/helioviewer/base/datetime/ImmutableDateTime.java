package org.helioviewer.base.datetime;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Class that holds Date and Time information. The data is stored in a Calendar
 * object. This class is immutable.
 *
 * @author caplins
 *
 */
public class ImmutableDateTime implements Comparable<ImmutableDateTime> {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

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
            calendar = Calendar.getInstance(UTC);
            calendar.clear();
            calendar.set(_year, _month, _day, _hour, _minute, _second);
            dateFormat.setTimeZone(UTC);
            timeFormat.setTimeZone(UTC);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ImmutableDateTime(long seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("Arguments cannot be negative!");
        }

        try {
            calendar = Calendar.getInstance(UTC);
            calendar.clear();
            calendar.setTimeInMillis(seconds * 1000);
            dateFormat.setTimeZone(UTC);
            timeFormat.setTimeZone(UTC);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ImmutableDateTime(ImmutableDateTime original) {
        if (original == null) {
            throw new IllegalArgumentException("Can not copy null object");
        }

        try {
            calendar = Calendar.getInstance(UTC);
            calendar.clear();
            calendar.setTimeInMillis(original.getMillis());
            dateFormat.setTimeZone(UTC);
            timeFormat.setTimeZone(UTC);
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
        if (cachedDate == null) {
            cachedDate = TimeUtils.utcDateFormat.format(calendar.getTime());
        }
        return cachedDate;
    }

    public static ImmutableDateTime parseDateTime(String dateTime) {
        int year = 0, month = 0, day = 0, hour = 0, minute = 0, second = 0;

        if (dateTime != null) {
            try {
                String[] firstDivide = dateTime.split("T");
                String[] secondDivide1 = firstDivide[0].split("[-/]");
                String[] secondDivide2 = firstDivide[1].split(":");
                String[] thirdDivide = secondDivide2[2].split("\\.");
                year = Integer.valueOf(secondDivide1[0]);
                month = Integer.valueOf(secondDivide1[1]);
                day = Integer.valueOf(secondDivide1[2]);
                hour = Integer.valueOf(secondDivide2[0]);
                minute = Integer.valueOf(secondDivide2[1]);
                second = Integer.valueOf(thirdDivide[0]);
            } catch (Exception e) {
                year = 0;
                month = 0;
                day = 0;
                hour = 0;
                minute = 0;
                second = 0;
            }
        }

        return new ImmutableDateTime(year, month != 0 ? month - 1 : 0, day, hour, minute, second);
    }

}
