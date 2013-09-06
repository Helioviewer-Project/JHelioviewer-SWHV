package org.helioviewer.jhv.gui.components.calendar;

import java.awt.Point;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Class manages a calendar view which shows all days of a month.
 * 
 * @author Stephan Pagel
 */
public class DayViewController implements CalendarViewController {

    // ////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////

    Calendar calendar = new GregorianCalendar();
    SimpleDateFormat dateFormat = new SimpleDateFormat("MMM yyyy");

    // ////////////////////////////////////////////////////////////////
    // Methods
    // ////////////////////////////////////////////////////////////////

    public DayViewController() {
        calendar.setFirstDayOfWeek(Calendar.SUNDAY);
        calendar.setMinimalDaysInFirstWeek(1);
    }

    /**
     * {@inheritDoc}
     * <p>
     * A period of one month will be added to the current date.
     */
    public Date moveForward() {

        calendar.add(Calendar.MONTH, 1);
        return calendar.getTime();
    }

    /**
     * {@inheritDoc}
     * <p>
     * A period of one month will be removed from the current date.
     */
    public Date moveBack() {

        calendar.add(Calendar.MONTH, -1);
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
     * <p>
     * The column headers are the short names of the weekdays.
     */
    public String[] getGridColumnHeader() {

        String[] values = new DateFormatSymbols().getShortWeekdays();
        String[] result = new String[7];

        for (int i = 1; i < 8; i++)
            result[i - 1] = values[i];

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Point getCorrespondingCellOfCurrentDate() {

        return new Point(calendar.get(Calendar.WEEK_OF_MONTH) - 1, calendar.get(Calendar.DAY_OF_WEEK) - 1);
    }

    /**
     * {@inheritDoc}
     */
    public void setDateOfCellValue(Object value) {

        if (value instanceof Integer) {
            calendar.set(Calendar.DAY_OF_MONTH, ((Integer) value).intValue());
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a 2 dimensional array where all days of a month are placed
     * against the corresponding weekday.
     */
    public Object[][] getGridData() {

        // compute number of days in selected month
        Calendar cal = (Calendar) calendar.clone();

        int numberOfDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // put all days at correct position in grid data
        Object[][] data = new Object[6][7];

        boolean win = System.getProperty("os.name").toLowerCase().contains("win");
        cal.set(Calendar.DAY_OF_MONTH, 1);

        // the calendar returns sometimes index 0 and sometimes index 1 for
        // first week of a month under windows, so an offset is needed
        int offset = 1;
        if (win)
            offset = cal.get(Calendar.WEEK_OF_MONTH);

        for (int i = 1; i <= numberOfDaysInMonth; i++) {

            cal.set(Calendar.DAY_OF_MONTH, i);

            data[cal.get(Calendar.WEEK_OF_MONTH) - offset][cal.get(Calendar.DAY_OF_WEEK) - 1] = new Integer(i);
        }

        return data;
    }

    /**
     * {@inheritDoc}
     */
    public Date getDate() {
        return calendar.getTime();
    }

    /**
     * {@inheritDoc}
     */
    public void setDate(Date date) {
        calendar.setTime(date);
    }
}
