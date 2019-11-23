package org.helioviewer.jhv.gui.components.timeselector;

import java.awt.Point;

// Interface which defines basic methods to manage the calendar view.
interface CalendarViewController {
    /**
     * Adds to the current selected time a defined time span.
     *
     * @return new time where time span is added.
     */
    long moveForward();

    /**
     * Removes from the current selected time a defined time span.
     *
     * @return new time where time span is removed.
     */
    long moveBack();

    /**
     * Returns the header text for the current view.
     *
     * @return current header text.
     */
    String getSelectionButtonText();

    /**
     * Returns the data which belongs to the calendar view of the current
     * selected time.
     *
     * @return data which can be displayed in a table grid.
     */
    Object[][] getGridData();

    /**
     * Returns the names of the columns.
     *
     * @return column names.
     */
    String[] getGridColumnHeader();

    /**
     * Returns the coordinates of the cell inside the table of the current
     * selected time.
     *
     * @return coordinate of the cell inside the table. The x coordinate
     * represents the row index, the y coordinate represents the column
     * index.
     */
    Point getCorrespondingCellOfCurrentTime();

    /**
     * Sets the current time by a given object value.
     *
     * @param value object which indicates the new time.
     * @see #getGridData()
     */
    void setTimeOfCellValue(Object value);

    /**
     * Sets the current selected time.
     */
    void setTime(long milli);

    /**
     * Returns the current selected time.
     *
     * @return selected time.
     */
    long getTime();

}
