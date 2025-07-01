package org.helioviewer.jhv.gui.components.timeselector;

import java.awt.Point;
import java.util.Calendar;
import java.util.GregorianCalendar;

// Class manages a calendar view which shows a period of 12 years
class YearViewController implements CalendarViewController {

    private final Calendar calendar = new GregorianCalendar();

    // A period of 10 years will be added to the current time.
    @Override
    public long moveForward() {
        calendar.add(Calendar.YEAR, 10);
        return calendar.getTimeInMillis();
    }

    // A period of 10 years will be removed from the current time.
    @Override
    public long moveBack() {
        calendar.add(Calendar.YEAR, -10);
        return calendar.getTimeInMillis();
    }

    @Override
    public String getSelectionButtonText() {
        int currentYear = calendar.get(Calendar.YEAR);
        return (currentYear - 6) + " - " + (currentYear + 5);
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

    // Returns a 2 dimensional array where all years of the 12 years time period
    // are placed.
    @Override
    public Object[][] getGridData() {
        Object[][] data = new Object[3][4];
        int year = calendar.get(Calendar.YEAR) - 6;

        for (int i = 0; i < 12; i++) {
            data[i / 4][i % 4] = year + i;
        }

        return data;
    }

    @Override
    public Point getCorrespondingCellOfCurrentTime() {
        return new Point(1, 2);
    }

    @Override
    public void setTimeOfCellValue(Object value) {
        if (value instanceof Integer v) {
            calendar.set(Calendar.YEAR, v);
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
