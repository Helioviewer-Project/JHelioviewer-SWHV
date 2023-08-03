package org.helioviewer.jhv.timelines;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.timelines.draw.ClickableDrawable;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.gui.TimelinePanel;

@SuppressWarnings("serial")
public class TimelineLayers extends AbstractTableModel {

    private static final ArrayList<TimelineLayer> layers = new ArrayList<>();
    private static final List<TimelineLayer> extLayers = Collections.unmodifiableList(layers);

    public static List<TimelineLayer> get() {
        return extLayers;
    }

    public void downloadStarted(TimelineLayer tl) {
        updateCell(layers.indexOf(tl), TimelinePanel.LOADING_COL);
    }

    public void downloadFinished(TimelineLayer tl) {
        updateCell(layers.indexOf(tl), TimelinePanel.LOADING_COL);
    }

    public void add(TimelineLayer tl) {
        if (layers.contains(tl)) // avoid band duplication via file load
            return;
        layers.add(tl);

        int row = layers.size() - 1;
        fireTableRowsInserted(row, row);
        DrawController.graphAreaChanged();
    }

    public void remove(TimelineLayer tl) {
        tl.remove();
        int row = layers.indexOf(tl);
        if (row == -1)
            return;

        layers.remove(tl);
        fireTableRowsDeleted(row, row);
        DrawController.graphAreaChanged();
    }

    public void clear() {
        layers.forEach(TimelineLayer::remove);
        layers.clear();
        fireTableDataChanged();
        DrawController.graphAreaChanged();
    }

    public void updateCell(int row, int col) {
        if (row >= 0) // negative row breaks model
            fireTableCellUpdated(row, col);
    }

    @Override
    public int getRowCount() {
        return layers.size();
    }

    @Override
    public int getColumnCount() {
        return TimelinePanel.NUMBEROFCOLUMNS;
    }

    @Nullable
    @Override
    public Object getValueAt(int row, int col) {
        try {
            return layers.get(row);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static ClickableDrawable getDrawableUnderMouse() {
        for (TimelineLayer tl : layers) {
            ClickableDrawable tlUnderMouse = tl.getDrawableUnderMouse();
            if (tlUnderMouse != null) {
                return tlUnderMouse;
            }
        }
        return null;
    }

    public static int getNumberOfYAxes() {
        int ct = 0;
        for (TimelineLayer tl : layers) {
            if (tl.showYAxis()) {
                ct++;
            }
        }
        return ct;
    }

    public static int getNumberOfPropagationAxes() {
        int ct = 0;
        for (TimelineLayer tl : layers) {
            if (tl.isPropagated()) {
                ct++;
            }
        }
        return ct;
    }

}
