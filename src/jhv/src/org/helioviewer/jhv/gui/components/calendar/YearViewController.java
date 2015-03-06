package org.helioviewer.jhv.gui.components.calendar;

import java.awt.Point;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Class manages a calendar view which shows the a period of 12 years. Each
 * number of a year of this 12 years is shown separately.
 * 
 * @author Stephan Pagel
 */
public class YearViewController implements CalendarViewController {

    // ////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////

    Calendar calendar = new GregorianCalendar();

    // ////////////////////////////////////////////////////////////////
    // Methods
    // ////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     * <p>
     * A period of 10 years will be added to the current date.
     */
    public Date moveForward() {

        calendar.add(Calendar.YEAR, 10);
        return calendar.getTime();
    }

    /**
     * {@inheritDoc}
     * <p>
     * A period of 10 years will be removed from the current date.
     */
    public Date moveBack() {

        calendar.add(Calendar.YEAR, -10);
        return calendar.getTime();
    }

    /**
     * {@inheritDoc}
     */
    public String getSelectionButtonText() {

        int currentYear = calendar.get(Calendar.YEAR);
        return Integer.toString(currentYear - 6) + " - " + Integer.toString(currentYear + 5);
    }

    /**
     * {@inheritDoc}
     */
    public Date getDate() {

        return calendar.getTime();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The column headers are numbered serially but this numbers represent no
     * special meaning (except uniqueness).
     */
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
    public Object[][] getGridData() {

        Object[][] data = new Object[3][4];

        int year = calendar.get(Calendar.YEAR) - 6;

        for (int i = 0; i < 12; i++) {
            data[i / 4][i % 4] = new Integer(year + i);
        }

        return data;
    }

    /**
     * {@inheritDoc}
     */
    public Point getCorrespondingCellOfCurrentDate() {

        return new Point(1, 2);
    }

    /**
     * {@inheritDoc}
     */
    public void setDateOfCellValue(Object value) {

        if (value instanceof Integer) {
            calendar.set(Calendar.YEAR, ((Integer) value).intValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setDate(Date date) {

        calendar.setTime(date);
    }
}
