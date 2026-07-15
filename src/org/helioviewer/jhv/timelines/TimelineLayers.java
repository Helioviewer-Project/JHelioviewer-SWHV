package org.helioviewer.jhv.timelines;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;

import javax.annotation.Nullable;
import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.timelines.draw.ClickableDrawable;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.draw.GraphGeometry;
import org.helioviewer.jhv.timelines.draw.TimeAxis;
import org.helioviewer.jhv.timelines.gui.TimelinePanel;

@SuppressWarnings("serial")
public class TimelineLayers extends AbstractTableModel {

    private static final ArrayList<TimelineLayer> layers = new ArrayList<>();
    private static final List<TimelineLayer> extLayers = Collections.unmodifiableList(layers);

    public static List<TimelineLayer> get() {
        return extLayers;
    }

    public static void draw(Graphics2D g, Rectangle graphArea, TimeAxis timeAxis, Point mousePosition) {
        GraphGeometry geometry = DrawController.getGeometry();
        boolean stackedMode = geometry.isStacked();
        int yAxisIndex = 0;

        for (TimelineLayer layer : layers) {
            if (!layer.isEnabled())
                continue;

            Rectangle area;
            if (stackedMode && layer.showYAxis()) {
                area = geometry.getLayerArea(yAxisIndex);
                yAxisIndex++;
            } else {
                area = graphArea;
            }

            g.setClip(area);
            layer.draw(g, area, timeAxis, mousePosition);
        }
        g.setClip(graphArea);
    }

    public static void fetchData(TimeAxis timeAxis) {
        layers.forEach(layer -> layer.fetchData(timeAxis));
    }

    public static boolean highlightChanged(Point p) {
        boolean changed = false;
        for (TimelineLayer tl : layers) {
            changed = tl.highlightChanged(p) || changed;
        }
        return changed;
    }

    public void updateRow(TimelineLayer tl) {
        int row = layers.indexOf(tl);
        if (row >= 0)
            fireTableRowsUpdated(row, row);
    }

    public void updateLoadingCell(TimelineLayer tl) {
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
        int row = layers.indexOf(tl);
        if (row == -1)
            return;

        tl.remove();
        layers.remove(tl);
        fireTableRowsDeleted(row, row);
        DrawController.graphAreaChanged();
    }

    public void restore(List<TimelineLayer> newLayers) {
        ArrayList<TimelineLayer> restoredLayers = new ArrayList<>();
        for (TimelineLayer layer : newLayers) {
            if (!restoredLayers.contains(layer)) // avoid duplicated bands in restored state
                restoredLayers.add(layer);
        }

        for (TimelineLayer layer : layers) {
            if (!restoredLayers.contains(layer))
                layer.remove();
        }

        layers.clear();
        layers.addAll(restoredLayers);
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

    @Override
    public Object getValueAt(int row, int col) {
        return layers.get(row);
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

    public static void forEachYAxis(ObjIntConsumer<TimelineLayer> consumer) {
        int axisIndex = -1;
        for (TimelineLayer tl : layers) {
            if (tl.showYAxis()) {
                consumer.accept(tl, axisIndex);
                axisIndex++;
            }
        }
    }

    public static int getNumberOfYAxes() {
        return count(TimelineLayer::showYAxis);
    }

    public static List<TimelineLayer> getVisibleYAxisLayers() {
        List<TimelineLayer> result = new ArrayList<>();
        for (TimelineLayer tl : layers) {
            if (tl.showYAxis() && tl.isEnabled()) {
                result.add(tl);
            }
        }
        return result;
    }

    @Nullable
    public static TimelineLayer getVisibleYAxisLayerAt(int index) {
        List<TimelineLayer> visible = getVisibleYAxisLayers();
        if (index >= 0 && index < visible.size()) {
            return visible.get(index);
        }
        return null;
    }

    public static Rectangle getDrawArea(TimelineLayer layer, Rectangle graphArea) {
        GraphGeometry geometry = DrawController.getGeometry();
        if (!geometry.isStacked()) {
            return graphArea;
        }
        if (!layer.showYAxis()) {
            return graphArea;
        }
        int index = 0;
        for (TimelineLayer tl : layers) {
            if (!tl.isEnabled() || !tl.showYAxis()) {
                continue;
            }
            if (tl == layer) {
                return geometry.getLayerArea(index);
            }
            index++;
        }
        return graphArea;
    }

    public static void forEachPropagated(ObjIntConsumer<TimelineLayer> consumer) {
        int index = 0;
        for (TimelineLayer tl : layers) {
            if (tl.isPropagated()) {
                consumer.accept(tl, index);
                index++;
            }
        }
    }

    public static int getNumberOfPropagationAxes() {
        return count(TimelineLayer::isPropagated);
    }

    public static boolean setYAxisHighlight(@Nullable GraphGeometry.YAxisHit hit) {
        boolean changed = false;
        int axisIndex = -1;
        for (TimelineLayer tl : layers) {
            if (tl.showYAxis()) {
                boolean highlighted = hit != null && hit.targets(axisIndex);
                changed = changed || tl.getYAxis().isHighlighted() != highlighted;
                tl.getYAxis().setHighlighted(highlighted);
                axisIndex++;
            }
        }
        return changed;
    }

    private static int count(Predicate<TimelineLayer> predicate) {
        int ct = 0;
        for (TimelineLayer tl : layers) {
            if (predicate.test(tl)) {
                ct++;
            }
        }
        return ct;
    }

}
