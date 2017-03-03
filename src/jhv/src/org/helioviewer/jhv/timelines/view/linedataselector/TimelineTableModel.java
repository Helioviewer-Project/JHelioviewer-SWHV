package org.helioviewer.jhv.timelines.view.linedataselector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.helioviewer.jhv.timelines.draw.ClickableDrawable;
import org.helioviewer.jhv.timelines.draw.DrawController;

public class TimelineTableModel implements TableModel {

    private final HashSet<TableModelListener> listeners = new HashSet<>();
    private final ArrayList<TimelineRenderable> elements = new ArrayList<>();

    public void downloadStarted(TimelineRenderable element) {
        fireUpdate(element, TimelinePanel.LOADING_COL);
    }

    public void downloadFinished(TimelineRenderable element) {
        fireUpdate(element, TimelinePanel.LOADING_COL);
    }

    public void addLineData(TimelineRenderable element) {
        elements.add(element);
        fireInsert(elements.size() - 1);
    }

    public void removeLineData(TimelineRenderable element) {
        element.remove();
        elements.remove(element);
        fireListeners();
    }

    public List<TimelineRenderable> getAllLineDataSelectorElements() {
        return elements;
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

    private void fireListeners() {
        TableModelEvent e = new TableModelEvent(this);
        for (TableModelListener listener : listeners) {
            listener.tableChanged(e);
        }
        DrawController.graphAreaChanged();
    }

    private void fireInsert(int idx) {
        TableModelEvent e = new TableModelEvent(this, idx, idx, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT);
        for (TableModelListener listener : listeners) {
            listener.tableChanged(e);
        }
        DrawController.graphAreaChanged();
    }

    void fireUpdate(TimelineRenderable element, int col) {
        int row = elements.indexOf(element);
        TableModelEvent e = new TableModelEvent(this, row, row, col, TableModelEvent.UPDATE);
        for (TableModelListener listener : listeners) {
            listener.tableChanged(e);
        }
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
    public String getColumnName(int columnIndex) {
        return Integer.toString(columnIndex);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return TimelineRenderable.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return elements.get(rowIndex);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
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
