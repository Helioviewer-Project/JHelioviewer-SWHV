package org.helioviewer.jhv.gui.components.timeselector;

import java.awt.Point;
import java.text.DateFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.helioviewer.jhv.time.TimeUtils;

// Class manages a calendar view which shows the months of a year.
class MonthViewController implements CalendarViewController {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy");

    private final Calendar calendar = new GregorianCalendar();

    // A period of one year will be added to the current time.
    @Override
    public long moveForward() {
        calendar.add(Calendar.YEAR, 1);
        return calendar.getTimeInMillis();
    }

    // A period of one year will be removed from the current time.
    @Override
    public long moveBack() {
        calendar.add(Calendar.YEAR, -1);
        return calendar.getTimeInMillis();
    }

    @Override
    public String getSelectionButtonText() {
        return TimeUtils.format(formatter, calendar.getTimeInMillis());
    }

    // The column headers are numbered serially but this numbers represent no
    // special meaning (except uniqueness).
    @Override
    public String[] getGridColumnHeader() {
        String[] names = new String[4];

        for (int i = 0; i < 4; i++)
            names[i] = Integer.toString(i);

        return names;
    }

    // Returns a 2 dimensional array where all month of a year are placed.
    @Override
    public Object[][] getGridData() {
        // get short names of the months
        String[] months = new DateFormatSymbols().getShortMonths();
        Object[][] data = new Object[3][4];

        // write names to table
        for (int i = 0; i < 12; i++) {
            data[i / 4][i % 4] = months[i];
        }

        return data;
    }

    @Override
    public Point getCorrespondingCellOfCurrentTime() {
        return new Point(calendar.get(Calendar.MONTH) / 4, calendar.get(Calendar.MONTH) % 4);
    }

    @Override
    public void setTimeOfCellValue(Object value) {
        if (value instanceof String) {
            String[] months = new DateFormatSymbols().getShortMonths();

            for (int i = 0; i < 12; i++) {
                if (months[i].equals(value)) {
                    calendar.set(Calendar.MONTH, i);
                }
            }
        }
    }

    @Override
    public void setTime(long milli) {
        calendar.setTimeInMillis(milli);
    }

    @Override
    public long getTime() {
        return calendar.getTimeInMillis();
    }

}
