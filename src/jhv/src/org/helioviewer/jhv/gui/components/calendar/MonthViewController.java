package org.helioviewer.jhv.gui.components.calendar;

import java.awt.Point;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Class manages a calendar view which shows the months of a year.
 * 
 * @author Stephan Pagel
 */
public class MonthViewController implements CalendarViewController {

    // ////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////

    Calendar calendar = new GregorianCalendar();
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy");

    // ////////////////////////////////////////////////////////////////
    // Methods
    // ////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     * <p>
     * A period of one year will be added to the current date.
     */
    public Date moveForward() {

        calendar.add(Calendar.YEAR, 1);
        return calendar.getTime();
    }

    /**
     * {@inheritDoc}
     * <p>
     * A period of one year will be removed from the current date.
     */
    public Date moveBack() {

        calendar.add(Calendar.YEAR, -1);
        return calendar.getTime();
    }

    /**
     * {@inheritDoc}
     */
    public String getSelectionButtonText() {

        return dateFormat.format(calendar.getTime());
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
     * Returns a 2 dimensional array where all month of a year are placed.
     */
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

    /**
     * {@inheritDoc}
     */
    public Point getCorrespondingCellOfCurrentDate() {

        return new Point(calendar.get(Calendar.MONTH) / 4, calendar.get(Calendar.MONTH) % 4);
    }

    /**
     * {@inheritDoc}
     */
    public void setDateOfCellValue(Object value) {

        if (value instanceof String) {

            String[] months = new DateFormatSymbols().getShortMonths();

            for (int i = 0; i < 12; i++) {

                if (months[i].equals((String) value)) {
                    calendar.set(Calendar.MONTH, i);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setDate(Date date) {

        calendar.setTime(date);
    }
}
