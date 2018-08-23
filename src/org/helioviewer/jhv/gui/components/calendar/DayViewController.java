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

    private final Calendar calendar = new GregorianCalendar();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM yyyy");

    public DayViewController() {
        calendar.setFirstDayOfWeek(Calendar.SUNDAY);
        calendar.setMinimalDaysInFirstWeek(1);
    }

    // A period of one month will be added to the current date.
    @Override
    public Date moveForward() {
        calendar.add(Calendar.MONTH, 1);
        return calendar.getTime();
    }

    // A period of one month will be removed from the current date
    @Override
    public Date moveBack() {
        calendar.add(Calendar.MONTH, -1);
        return calendar.getTime();
    }

    @Override
    public String getSelectionButtonText() {
        return dateFormat.format(calendar.getTime());
    }

    // The column headers are the short names of the weekdays
    @Override
    public String[] getGridColumnHeader() {
        String[] result = new String[7];
        System.arraycopy(new DateFormatSymbols().getShortWeekdays(), 1, result, 0, 7);

        return result;
    }

    @Override
    public Point getCorrespondingCellOfCurrentDate() {
        return new Point(calendar.get(Calendar.WEEK_OF_MONTH) - 1, calendar.get(Calendar.DAY_OF_WEEK) - 1);
    }

    @Override
    public void setDateOfCellValue(Object value) {
        if (value instanceof Integer) {
            calendar.set(Calendar.DAY_OF_MONTH, (Integer) value);
        }
    }

    // Returns a 2 dimensional array where all days of a month are placed against the corresponding weekday
    @Override
    public Object[][] getGridData() {
        // compute number of days in selected month
        Calendar cal = (Calendar) calendar.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        // the calendar returns sometimes index 0 and sometimes index 1 for
        // first week of a month under windows, so an offset is needed
        int offset = 1;
        if (System.getProperty("jhv.os").equals("windows"))
            offset = cal.get(Calendar.WEEK_OF_MONTH);

        // put all days at correct position in grid data
        int numberOfDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        Object[][] data = new Object[6][7];
        for (int i = 1; i <= numberOfDaysInMonth; i++) {
            cal.set(Calendar.DAY_OF_MONTH, i);
            data[cal.get(Calendar.WEEK_OF_MONTH) - offset][cal.get(Calendar.DAY_OF_WEEK) - 1] = i;
        }

        return data;
    }

    @Override
    public Date getDate() {
        return calendar.getTime();
    }

    @Override
    public void setDate(Date date) {
        calendar.setTime(date);
    }

}
