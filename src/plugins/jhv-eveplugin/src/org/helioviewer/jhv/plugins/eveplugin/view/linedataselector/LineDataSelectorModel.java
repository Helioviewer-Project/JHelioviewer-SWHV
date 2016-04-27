package org.helioviewer.jhv.plugins.eveplugin.view.linedataselector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.helioviewer.jhv.plugins.eveplugin.lines.data.Band;
import org.helioviewer.jhv.plugins.eveplugin.lines.data.BandColors;
import org.helioviewer.jhv.plugins.eveplugin.settings.BandType;

public class LineDataSelectorModel implements TableModel {

    private static final HashSet<LineDataSelectorModelListener> listeners = new HashSet<LineDataSelectorModelListener>();
    private static final HashSet<TableModelListener> tableListeners = new HashSet<TableModelListener>();

    private static final ArrayList<LineDataSelectorElement> elements = new ArrayList<LineDataSelectorElement>();

    public final static int NUMBEROFCOLUMNS = 5;

    public void addLineDataSelectorModelListener(LineDataSelectorModelListener listener) {
        listeners.add(listener);
    }

    public void removeLineDataSelectorModelListener(LineDataSelectorModelListener listener) {
        listeners.remove(listener);
    }

    public void downloadStarted(LineDataSelectorElement element) {
        fireListeners();
    }

    public void downloadFinished(LineDataSelectorElement element) {
        fireListeners();
    }

    public int getNumberOfAvailableLineData() {
        return elements.size();
    }

    public void addLineData(LineDataSelectorElement element) {
        elements.add(element);
        fireLineDataSelectorElementAdded(element);
        fireListeners();
    }

    public List<LineDataSelectorElement> getAllLineDataSelectorElements() {
        return elements;
    }

    public void removeLineData(LineDataSelectorElement element) {
        elements.remove(element);
        BandColors.resetColor(element.getDataColor());

        fireLineDataSelectorElementRemoved(element);
        fireListeners();
    }

    public void lineDataElementUpdated(LineDataSelectorElement element) {
        fireLineDataSelectorElementUpdated(element);
    }

    public boolean atLeastOneDownloading() {
        for (LineDataSelectorElement el : elements) {
            if (el.isDownloading()) {
                return true;
            }
        }
        return false;
    }

    private void fireListeners() {
        TableModelEvent e = new TableModelEvent(this);
        for (TableModelListener listener : tableListeners) {
            listener.tableChanged(e);
        }
    }

    private void fireLineDataSelectorElementRemoved(LineDataSelectorElement element) {
        for (LineDataSelectorModelListener listener : listeners) {
            listener.lineDataRemoved(element);
        }
    }

    private void fireLineDataSelectorElementAdded(LineDataSelectorElement element) {
        for (LineDataSelectorModelListener listener : listeners) {
            listener.lineDataAdded(element);
        }
    }

    private void fireLineDataSelectorElementUpdated(LineDataSelectorElement element) {
        for (LineDataSelectorModelListener listener : listeners) {
            listener.lineDataUpdated(element);
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
        return String.valueOf(columnIndex);
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

    public void removeRow(int row) {
        LineDataSelectorElement el = elements.get(row);
        el.removeLineData();
    }

    public boolean containsBandType(BandType bandType) {
        for (LineDataSelectorElement el : elements) {
            if (el instanceof Band) {
                Band band = (Band) el;
                if (band.getBandType().equals(bandType))
                    return true;
            }

        }
        return false;
    }

    public int getNumberOfAxes() {
        int ct = 0;
        for (LineDataSelectorElement el : elements) {
            if (el.showYAxis()) {
                ct++;
            }
        }
        return ct;
    }

}
