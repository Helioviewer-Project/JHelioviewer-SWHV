package org.helioviewer.jhv.data.container.util;

import java.util.Calendar;
import java.util.Date;

public class DateUtil {
    /**
     * Gets the date of give date with hour, minute, seconds, milliseconds to 0.
     * 
     * @param date
     *            the date to round
     * @return the rounded date
     */
    public static Date getCurrentDate(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    /**
     * Gets the date rounded up to the following day. So day+1, hour, minute,
     * second, millisecond 0.
     * 
     * @param date
     *            The date to round up.
     * @return The rounded up date on the day
     */
    public static Date getNextDate(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }
}
