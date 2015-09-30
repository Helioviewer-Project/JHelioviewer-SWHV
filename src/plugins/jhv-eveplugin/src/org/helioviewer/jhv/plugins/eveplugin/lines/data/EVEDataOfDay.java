package org.helioviewer.jhv.plugins.eveplugin.lines.data;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class EVEDataOfDay {

    private final int MINUTES_PER_DAY = 1440;

    private final float[] values = new float[MINUTES_PER_DAY];
    private final long[] dates = new long[MINUTES_PER_DAY];
    private boolean hasData = false;

    public EVEDataOfDay(final int year, final int month, final int dayOfMonth) {
        GregorianCalendar calendar = new GregorianCalendar(year, month, dayOfMonth);

        Arrays.fill(values, Float.NaN);
        for (int i = 0; i < values.length; i++) {
            dates[i] = calendar.getTime().getTime();
            calendar.add(Calendar.MINUTE, 1);
        }
    }

    public void setValue(final int minuteOfDay, final float value, final long date) {
        values[minuteOfDay] = value;
        hasData = true;
    }

    public void fillResult(EVEValues result) {
        result.addValues(dates, values);
    }

    public boolean hasData() {
        return hasData;
    }

}