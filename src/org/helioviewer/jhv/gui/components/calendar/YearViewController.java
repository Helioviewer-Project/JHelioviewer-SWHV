package org.helioviewer.jhv.gui.components.calendar;

import java.awt.Point;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Class manages a calendar view which shows the a period of 12 years. Each
 * number of a year of this 12 years is shown separately.
 *
 * @author Stephan Pagel
 */
public class YearViewController implements CalendarViewController {

    private final Calendar calendar = new GregorianCalendar();

    /**
     * {@inheritDoc}
     * <p>
     * A period of 10 years will be added to the current time.
     */
    @Override
    public long moveForward() {
        calendar.add(Calendar.YEAR, 10);
        return calendar.getTimeInMillis();
    }

    /**
     * {@inheritDoc}
     * <p>
     * A period of 10 years will be removed from the current time.
     */
    @Override
    public long moveBack() {
        calendar.add(Calendar.YEAR, -10);
        return calendar.getTimeInMillis();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSelectionButtonText() {
        int currentYear = calendar.get(Calendar.YEAR);
        return (currentYear - 6) + " - " + (currentYear + 5);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The column headers are numbered serially but this numbers represent no
     * special meaning (except uniqueness).
     */
    @Override
    public String[] getGridColumnHeader() {
        String[] names = new String[4];

        for (int i = 0; i < 4; i++)
            names[i] = Integer.toString(i);

        return names;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a 2 dimensional array where all years of the 12 years time period
     * are placed.
     */
    @Override
    public Object[][] getGridData() {
        Object[][] data = new Object[3][4];
        int year = calendar.get(Calendar.YEAR) - 6;

        for (int i = 0; i < 12; i++) {
            data[i / 4][i % 4] = year + i;
        }

        return data;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Point getCorrespondingCellOfCurrentTime() {
        return new Point(1, 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTimeOfCellValue(Object value) {
        if (value instanceof Integer) {
            calendar.set(Calendar.YEAR, (Integer) value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTime(long milli) {
        calendar.setTimeInMillis(milli);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTime() {
        return calendar.getTimeInMillis();
    }

}
