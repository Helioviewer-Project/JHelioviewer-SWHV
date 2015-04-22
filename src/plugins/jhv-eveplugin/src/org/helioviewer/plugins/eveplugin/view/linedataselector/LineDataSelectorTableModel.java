package org.helioviewer.plugins.eveplugin.view.linedataselector;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

public class LineDataSelectorTableModel implements TableModel, LineDataSelectorModelListener {

    public final static int NUMBEROFCOLUMNS = 4;
    private final LineDataSelectorModel lineDataSelectorModel;
    private final List<TableModelListener> listeners;

    public LineDataSelectorTableModel() {
        lineDataSelectorModel = LineDataSelectorModel.getSingletonInstance();
        listeners = new ArrayList<TableModelListener>();
        lineDataSelectorModel.addLineDataSelectorModelListener(this);
    }

    @Override
    public void downloadStartded(LineDataSelectorElement element) {
        // TODO Auto-generated method stub

    }

    @Override
    public void downloadFinished(LineDataSelectorElement element) {
        // TODO Auto-generated method stub

    }

    @Override
    public void lineDataAdded(LineDataSelectorElement element) {
        // TODO Auto-generated method stub

    }

    @Override
    public void lineDataRemoved(LineDataSelectorElement element) {
        // TODO Auto-generated method stub

    }

    @Override
    public void lineDataUpdated(LineDataSelectorElement element) {
        // TODO Auto-generated method stub

    }

    @Override
    public int getRowCount() {
        return lineDataSelectorModel.getAllLineDataSelectorElements().size();
    }

    @Override
    public int getColumnCount() {
        return NUMBEROFCOLUMNS;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return "" + columnIndex;
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
        return lineDataSelectorModel.getAllLineDataSelectorElements().get(rowIndex);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        lineDataSelectorModel.addLineData((LineDataSelectorElement) aValue, rowIndex);
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }
}
