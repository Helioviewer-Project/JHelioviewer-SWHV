package org.helioviewer.jhv.timelines.view.linedataselector;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.timelines.draw.ClickableDrawable;
import org.helioviewer.jhv.timelines.draw.DrawController;

@SuppressWarnings("serial")
public class TimelineTableModel extends AbstractTableModel {

    private final ArrayList<TimelineRenderable> elements = new ArrayList<>();

    public List<TimelineRenderable> getAllLineDataSelectorElements() {
        return elements;
    }

    public void downloadStarted(TimelineRenderable element) {
        updateCell(elements.indexOf(element), TimelinePanel.LOADING_COL);
    }

    public void downloadFinished(TimelineRenderable element) {
        updateCell(elements.indexOf(element), TimelinePanel.LOADING_COL);
    }

    public void addLineData(TimelineRenderable element) {
        elements.add(element);

        int row = elements.size() - 1;
        fireTableRowsInserted(row, row);
        DrawController.graphAreaChanged();
    }

    public void removeLineData(TimelineRenderable element) {
        element.remove();
        int row = elements.indexOf(element);
        elements.remove(element);

        fireTableRowsDeleted(row, row);
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
        for (TimelineRenderable el : elements) {
            ClickableDrawable elUnderMouse = el.getDrawableUnderMouse();
            if (elUnderMouse != null) {
                return elUnderMouse;
            }
        }
        return null;
    }

    public int getNumberOfAxes() {
        int ct = 0;
        for (TimelineRenderable el : elements) {
            if (el.showYAxis()) {
                ct++;
            }
        }
        return ct;
    }

}
