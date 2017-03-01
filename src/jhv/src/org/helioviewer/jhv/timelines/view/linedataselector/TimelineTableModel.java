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

    private static final HashSet<TimelineTableModelListener> listeners = new HashSet<>();
    private static final HashSet<TableModelListener> tableListeners = new HashSet<>();
    private static final ArrayList<TimelineRenderable> elements = new ArrayList<>();

    private static final int NUMBEROFCOLUMNS = 5;

    public static void addLineDataSelectorModelListener(TimelineTableModelListener listener) {
        listeners.add(listener);
    }

    public static void removeLineDataSelectorModelListener(TimelineTableModelListener listener) {
        listeners.remove(listener);
    }

    public static void downloadStarted(TimelineRenderable element) {
        fireListeners();
    }

    public static void downloadFinished(TimelineRenderable element) {
        fireListeners();
    }

    public static void addLineData(TimelineRenderable element) {
        elements.add(element);
        fireLineDataSelectorElementAdded(element);
        fireListeners();
    }

    public static List<TimelineRenderable> getAllLineDataSelectorElements() {
        return elements;
    }

    public static void removeLineData(TimelineRenderable element) {
        elements.remove(element);
        fireLineDataSelectorElementRemoved(element);
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
        for (TableModelListener listener : tableListeners) {
            listener.tableChanged(e);
        }
    }

    private static void fireLineDataSelectorElementRemoved(TimelineRenderable element) {
        for (TimelineTableModelListener listener : listeners) {
            listener.lineDataRemoved();
        }
    }

    private static void fireLineDataSelectorElementAdded(TimelineRenderable element) {
        for (TimelineTableModelListener listener : listeners) {
            listener.lineDataAdded(element);
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
        elements.add(rowIndex, (TimelineRenderable) aValue);
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        tableListeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        tableListeners.remove(l);
    }

    public static void removeRow(int row) {
        TimelineRenderable el = elements.get(row);
        el.removeLineData();
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

    static int getRowIndex(TimelineRenderable element) {
        return elements.indexOf(element);
    }

}
