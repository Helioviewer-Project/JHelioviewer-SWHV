package org.helioviewer.jhv.base.time;

import java.util.Calendar;
import java.util.Date;

public class JHVDate implements Comparable<JHVDate> {

    private final String string;
    public final long milli;

    /**
     * No arguments may be negative or an exception will be thrown.
     * NOTE: Month argument is zero based... i.e. January corresponds to 0
     */
    public JHVDate(int _year, int _month, int _day, int _hour, int _minute, int _second) {
        if ((_year | _month | _day | _hour | _minute | _second) < 0)
            throw new IllegalArgumentException("Arguments cannot be negative!");

        Calendar c = Calendar.getInstance(TimeUtils.UTC);
        c.clear();
        c.set(_year, _month, _day, _hour, _minute, _second);

        Date date = c.getTime();
        string = TimeUtils.utcDateFormat.format(date);
        milli = date.getTime();
    }

    public JHVDate(long millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("Arguments cannot be negative!");
        }

        Calendar c = Calendar.getInstance(TimeUtils.UTC);
        c.clear();
        c.setTimeInMillis(millis);

        Date date = c.getTime();
        string = TimeUtils.utcDateFormat.format(date);
        milli = date.getTime();
    }

    public Date getDate() {
        return new Date(milli);
    }

    @Override
    public int compareTo(JHVDate dt) {
        long diff = (milli - dt.milli);
        return diff < 0 ? -1 : (diff > 0 ? +1 : 0);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof JHVDate)) {
            return false;
        }
        JHVDate dt = (JHVDate) o;
        return milli == dt.milli;
    }

    @Override
    public int hashCode() {
        return (int) (milli ^ (milli >>> 32));
    }

    @Override
    public final String toString() {
        return string;
    }

    public static JHVDate parseDateTime(String dateTime) {
        int year = 0, month = 0, day = 0, hour = 0, minute = 0, second = 0;

        if (dateTime != null) {
            try {
                String[] firstDivide = dateTime.split("T");
                String[] secondDivide1 = firstDivide[0].split("[-/]");
                String[] secondDivide2 = firstDivide[1].split(":");
                String[] thirdDivide = secondDivide2[2].split("\\.");
                year = Integer.parseInt(secondDivide1[0]);
                month = Integer.parseInt(secondDivide1[1]);
                day = Integer.parseInt(secondDivide1[2]);
                hour = Integer.parseInt(secondDivide2[0]);
                minute = Integer.parseInt(secondDivide2[1]);
                second = Integer.parseInt(thirdDivide[0]);
            } catch (Exception e) {
                year = 0;
                month = 0;
                day = 0;
                hour = 0;
                minute = 0;
                second = 0;
            }
        }
        return new JHVDate(year, month != 0 ? month - 1 : 0, day, hour, minute, second);
    }

}
