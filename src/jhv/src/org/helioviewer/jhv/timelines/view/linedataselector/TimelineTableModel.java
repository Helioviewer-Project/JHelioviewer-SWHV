package org.helioviewer.jhv.timelines.view.linedataselector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.draw.ClickableDrawable;

public class TimelineTableModel implements TableModel {

    private static final HashSet<TableModelListener> listeners = new HashSet<>();
    private static final ArrayList<TimelineRenderable> elements = new ArrayList<>();

    private static final int NUMBEROFCOLUMNS = 5;

    public static void downloadStarted(TimelineRenderable element) {
        fireListeners();
    }

    public static void downloadFinished(TimelineRenderable element) {
        fireListeners();
    }

    public static void addLineData(TimelineRenderable element) {
        elements.add(element);
        fireInsert(elements.size() - 1);
    }

    public static List<TimelineRenderable> getAllLineDataSelectorElements() {
        return elements;
    }

    public static void removeLineData(TimelineRenderable element) {
        elements.remove(element);
        fireListeners();
    }

    public static ClickableDrawable getElementUnderMouse() {
        for (TimelineRenderable el : elements) {
            ClickableDrawable elUnderMouse = el.getElementUnderMouse();
            if (elUnderMouse != null) {
                return elUnderMouse;
            }
        }
        return null;
    }

    private static void fireListeners() {
        TableModelEvent e = new TableModelEvent(Timelines.ldsm);
        for (TableModelListener listener : listeners) {
            listener.tableChanged(e);
        }
    }

    private static void fireInsert(int idx) {
        TableModelEvent e = new TableModelEvent(Timelines.ldsm, idx, idx, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT);
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
        return NUMBEROFCOLUMNS;
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

    static void removeRow(int row) {
        elements.get(row).removeLineData();
    }

    public static int getNumberOfAxes() {
        int ct = 0;
        for (TimelineRenderable el : elements) {
            if (el.showYAxis()) {
                ct++;
            }
        }
        return ct;
    }

}
