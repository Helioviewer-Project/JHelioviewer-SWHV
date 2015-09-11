package org.helioviewer.jhv.plugins.eveplugin.view.linedataselector;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.helioviewer.jhv.plugins.eveplugin.draw.DrawControllerListener;
import org.helioviewer.jhv.plugins.eveplugin.draw.PlotAreaSpace;
import org.helioviewer.jhv.plugins.eveplugin.draw.PlotAreaSpaceListener;
import org.helioviewer.jhv.plugins.eveplugin.lines.data.BandColors;

public class LineDataSelectorModel implements TableModel, PlotAreaSpaceListener, DrawControllerListener {

    private final List<LineDataSelectorModelListener> listeners;
    private final List<LineDataSelectorElement> elements;
    public final static int NUMBEROFCOLUMNS = 5;
    private final List<TableModelListener> tableListeners;

    private static LineDataSelectorModel instance;

    private LineDataSelectorModel() {
        listeners = new ArrayList<LineDataSelectorModelListener>();
        elements = new ArrayList<LineDataSelectorElement>();
        tableListeners = new ArrayList<TableModelListener>();

    }

    public static LineDataSelectorModel getSingletonInstance() {
        if (instance == null) {
            instance = new LineDataSelectorModel();
            PlotAreaSpace.getSingletonInstance().addPlotAreaSpaceListener(instance);
        }
        return instance;
    }

    public void addLineDataSelectorModelListener(LineDataSelectorModelListener listener) {
        listeners.add(listener);
    }

    public void removeLineDataSelectorModelListener(LineDataSelectorModelListener listener) {
        listeners.remove(listener);
    }

    public void downloadStarted(LineDataSelectorElement element) {
        fireDownloadStarted(element);
        fireListeners();
    }

    public void downloadFinished(LineDataSelectorElement element) {
        fireDownloadFinished(element);
        fireListeners();
    }

    public int getNumberOfAvailableLineData() {
        return elements.size();
    }

    public void addLineData(LineDataSelectorElement element) {
        elements.add(element);
        element.setDataColor(BandColors.getNextColor());
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

    public void addLineData(LineDataSelectorElement element, int rowIndex) {
        elements.add(rowIndex, element);
        fireListeners();
    }

    private void fireListeners() {
        for (TableModelListener listener : tableListeners) {
            TableModelEvent e = new TableModelEvent(this);
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

    private void fireDownloadStarted(LineDataSelectorElement element) {
        for (LineDataSelectorModelListener listener : listeners) {
            listener.downloadStartded(element);
        }
    }

    private void fireDownloadFinished(LineDataSelectorElement element) {
        for (LineDataSelectorModelListener listener : listeners) {
            listener.downloadFinished(element);
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

    @Override
    public void plotAreaSpaceChanged(double scaledMinValue, double scaledMaxValue, double scaledMinTime, double scaledMaxTime, double scaledSelectedMinValue, double scaledSelectedMaxValue, double scaledSelectedMinTime, double scaledSelectedMaxTime, boolean forced) {
        fireListeners();
    }

    @Override
    public void availablePlotAreaSpaceChanged(double oldMinValue, double oldMaxValue, double oldMinTime, double oldMaxTime, double newMinValue, double newMaxValue, double newMinTime, double newMaxTime) {
        // TODO Auto-generated method stub
    }

    @Override
    public void drawRequest() {
        fireListeners();
    }

    @Override
    public void drawMovieLineRequest(Date time) {
        // TODO Auto-generated method stub
    }

}
