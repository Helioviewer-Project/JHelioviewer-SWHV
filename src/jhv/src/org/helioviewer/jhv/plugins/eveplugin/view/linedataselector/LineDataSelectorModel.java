package org.helioviewer.jhv.plugins.eveplugin.view.linedataselector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.helioviewer.jhv.plugins.eveplugin.Timelines;
import org.helioviewer.jhv.plugins.eveplugin.draw.ClickableDrawable;

public class LineDataSelectorModel implements TableModel {

    private static final HashSet<LineDataSelectorModelListener> listeners = new HashSet<>();
    private static final HashSet<TableModelListener> tableListeners = new HashSet<>();
    private static final ArrayList<LineDataSelectorElement> elements = new ArrayList<>();

    private static final int NUMBEROFCOLUMNS = 5;

    public static void addLineDataSelectorModelListener(LineDataSelectorModelListener listener) {
        listeners.add(listener);
    }

    public static void removeLineDataSelectorModelListener(LineDataSelectorModelListener listener) {
        listeners.remove(listener);
    }

    public static void downloadStarted(LineDataSelectorElement element) {
        fireListeners();
    }

    public static void downloadFinished(LineDataSelectorElement element) {
        fireListeners();
    }

    public static void addLineData(LineDataSelectorElement element) {
        elements.add(element);
        fireLineDataSelectorElementAdded(element);
        fireListeners();
    }

    public static List<LineDataSelectorElement> getAllLineDataSelectorElements() {
        return elements;
    }

    public static void removeLineData(LineDataSelectorElement element) {
        elements.remove(element);
        fireLineDataSelectorElementRemoved(element);
        fireListeners();
    }

    public static ClickableDrawable getElementUnderMouse() {
        for (LineDataSelectorElement el : elements) {
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

    private static void fireLineDataSelectorElementRemoved(LineDataSelectorElement element) {
        for (LineDataSelectorModelListener listener : listeners) {
            listener.lineDataRemoved();
        }
    }

    private static void fireLineDataSelectorElementAdded(LineDataSelectorElement element) {
        for (LineDataSelectorModelListener listener : listeners) {
            listener.lineDataAdded(element);
        }
    }

    static void fireLineDataSelectorElementVisibility(LineDataSelectorElement element, boolean flag) {
        for (LineDataSelectorModelListener listener : listeners) {
            listener.lineDataVisibility();
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
        return LineDataSelectorElement.class;
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
        elements.add(rowIndex, (LineDataSelectorElement) aValue);
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
        LineDataSelectorElement el = elements.get(row);
        el.removeLineData();
    }

    public static int getNumberOfAxes() {
        int ct = 0;
        for (LineDataSelectorElement el : elements) {
            if (el.showYAxis()) {
                ct++;
            }
        }
        return ct;
    }

    static int getRowIndex(LineDataSelectorElement element) {
        return elements.indexOf(element);
    }

}
