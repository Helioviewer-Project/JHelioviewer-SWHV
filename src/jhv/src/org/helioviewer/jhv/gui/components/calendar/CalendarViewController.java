package org.helioviewer.jhv.gui.components.calendar;

import java.awt.Point;
import java.util.Date;

/**
 * Interface which defines basic methods to manage the calendar view.
 * 
 * @see JHVCalendar
 * 
 * @author Stephan Pagel
 */
public interface CalendarViewController {

    /**
     * Adds to the current selected date a defined time span.
     * 
     * @return new date where time span is added.
     */
    public Date moveForward();

    /**
     * Removes from the current selected date a defined time span.
     * 
     * @return new date where time span is removed.
     */
    public Date moveBack();

    /**
     * Returns the header text for the current view.
     * 
     * @return current header text.
     */
    public String getSelectionButtonText();

    /**
     * Returns the data which belongs to the calendar view an the current
     * selected date.
     * 
     * @return data which can be displayed in a table grid.
     */
    public Object[][] getGridData();

    /**
     * Returns the names of the columns.
     * 
     * @return column names.
     */
    public String[] getGridColumnHeader();

    /**
     * Returns the coordinates of the cell inside the table of the current
     * selected date.
     * 
     * @return coordinate of the cell inside the table. The x coordinate
     *         represents the row index, the y coordinate represents the column
     *         index.
     */
    public Point getCorrespondingCellOfCurrentDate();

    /**
     * Sets the current date by a given object value.
     * 
     * @see #getGridData()
     * 
     * @param value
     *            object which indicates the new date.
     */
    public void setDateOfCellValue(Object value);

    /**
     * Sets the current selected date.
     * 
     * @param date
     *            new selected date.
     */
    public void setDate(Date date);

    /**
     * Returns the current selected date.
     * 
     * @return selected date.
     */
    public Date getDate();
}
