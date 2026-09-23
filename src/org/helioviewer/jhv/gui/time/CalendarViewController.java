package org.helioviewer.jhv.gui.time;

import java.awt.Point;

// Interface which defines basic methods to manage the calendar view.
interface CalendarViewController {
    // Moves the selected time one view span forward and returns the new time.
    long moveForward();

    // Moves the selected time one view span back and returns the new time.
    long moveBack();

    String getSelectionButtonText();

    // Grid of the view containing the selected time.
    Object[][] getGridData();

    String[] getGridColumnHeader();

    // Cell of the selected time: x is the row, y is the column.
    Point getCorrespondingCellOfCurrentTime();

    // Sets the time from a cell value of getGridData().
    void setTimeOfCellValue(Object value);

    void setTime(long milli);

    long getTime();

}
