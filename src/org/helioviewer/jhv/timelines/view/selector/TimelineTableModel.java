package org.helioviewer.jhv.timelines.view.selector;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.timelines.TimelineLayer;
import org.helioviewer.jhv.timelines.draw.ClickableDrawable;
import org.helioviewer.jhv.timelines.draw.DrawController;

@SuppressWarnings("serial")
public class TimelineTableModel extends AbstractTableModel {

    private ArrayList<TimelineLayer> elements = new ArrayList<>();

    public List<TimelineLayer> getAllLineDataSelectorElements() {
        return elements;
    }

    public void downloadStarted(TimelineLayer element) {
        updateCell(elements.indexOf(element), TimelinePanel.LOADING_COL);
    }

    public void downloadFinished(TimelineLayer element) {
        updateCell(elements.indexOf(element), TimelinePanel.LOADING_COL);
    }

    public void addLineData(TimelineLayer element) {
        if (elements.contains(element)) // avoid band duplication via file load
            return;
        elements.add(element);

        int row = elements.size() - 1;
        fireTableRowsInserted(row, row);
        DrawController.graphAreaChanged();
    }

    public void removeLineData(TimelineLayer element) {
        element.remove();
        int row = elements.indexOf(element);
        if (row == -1)
            return;

        elements.remove(element);
        fireTableRowsDeleted(row, row);
        DrawController.graphAreaChanged();
    }

    public void clear() {
        for (TimelineLayer element : elements) {
            element.remove();
        }
        elements = new ArrayList<>();
        fireTableDataChanged();
        DrawController.graphAreaChanged();
    }

    void updateCell(int row, int col) {
        if (row >= 0) // negative row breaks model
            fireTableCellUpdated(row, col);
    }

    @Override
    public int getRowCount() {
        return elements.size();
    }

    @Override
    public int getColumnCount() {
        return TimelinePanel.NUMBEROFCOLUMNS;
    }

    @Override
    public Object getValueAt(int row, int col) {
        try {
            return elements.get(row);
        } catch (Exception e) {
            return null;
        }
    }

    public ClickableDrawable getDrawableUnderMouse() {
        for (TimelineLayer tl : elements) {
            ClickableDrawable elUnderMouse = tl.getDrawableUnderMouse();
            if (elUnderMouse != null) {
                return elUnderMouse;
            }
        }
        return null;
    }

    public int getNumberOfAxes() {
        int ct = 0;
        for (TimelineLayer tl : elements) {
            if (tl.showYAxis()) {
                ct++;
            }
        }
        return ct;
    }

}
