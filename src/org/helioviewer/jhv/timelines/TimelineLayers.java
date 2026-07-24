package org.helioviewer.jhv.timelines;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.timelines.band.Band;
import org.helioviewer.jhv.timelines.band.BandType;
import org.helioviewer.jhv.timelines.draw.ClickableDrawable;
import org.helioviewer.jhv.timelines.draw.DrawController;
import org.helioviewer.jhv.timelines.draw.GraphGeometry;
import org.helioviewer.jhv.timelines.draw.TimeAxis;

@SuppressWarnings("serial")
public class TimelineLayers extends AbstractTableModel {

    public static final int ENABLED_COLUMN = 0;
    public static final int TITLE_COLUMN = 1;
    public static final int LOADING_COLUMN = 2;
    public static final int APPEARANCE_COLUMN = 3;
    public static final int REMOVE_COLUMN = 4;
    public static final int COLUMN_COUNT = 5;

    private static final ArrayList<TimelineLayer> layers = new ArrayList<>();
    private static final List<TimelineLayer> extLayers = Collections.unmodifiableList(layers);

    public static List<TimelineLayer> get() {
        return extLayers;
    }

    public static void draw(Graphics2D g, Rectangle graphArea, TimeAxis timeAxis, Point mousePosition) {
        GraphGeometry geometry = DrawController.getGeometry();
        boolean stackedMode = geometry.isStacked();
        boolean warningBandDrawn = false;

        for (TimelineLayer layer : layers) {
            if (!layer.isEnabled())
                continue;

            Rectangle area = graphArea;
            if (layer.hasYAxis()) {
                area = geometry.getLayerArea(layer);
                if (area == null)
                    continue;
            }

            g.setClip(area);
            if (layer instanceof Band band) {
                boolean drawWarnings = stackedMode || !warningBandDrawn;
                warningBandDrawn |= band.hasWarningLevels();
                band.draw(g, area, timeAxis, mousePosition, drawWarnings);
            } else {
                layer.draw(g, area, timeAxis, mousePosition);
            }
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
        updateCell(layers.indexOf(tl), LOADING_COLUMN);
    }

    public Band getOrCreateBand(BandType bandType) {
        Band band = findBand(layers, bandType);
        return band == null ? new Band(bandType) : band;
    }

    public Band addBand(BandType bandType) {
        Band band = getOrCreateBand(bandType);
        add(band);
        return band;
    }

    public void add(TimelineLayer tl) {
        if (containsLayer(layers, tl))
            return;
        layers.add(tl);

        configureLayer(tl);

        int row = layers.size() - 1;
        fireTableRowsInserted(row, row);
        DrawController.layoutChanged();
        tl.fetchData(DrawController.selectedAxis);
    }

    public void remove(TimelineLayer tl) {
        int row = layers.indexOf(tl);
        if (row == -1)
            return;

        tl.remove();
        layers.remove(row);
        fireTableRowsDeleted(row, row);
        DrawController.layoutChanged();
    }

    public void restore(List<TimelineLayer> newLayers) {
        ArrayList<TimelineLayer> restoredLayers = new ArrayList<>();
        for (TimelineLayer layer : newLayers)
            addUnique(restoredLayers, layer);
        replaceAll(restoredLayers);
    }

    public void replaceBands(List<BandType> bandTypes) {
        ArrayList<TimelineLayer> replacement = new ArrayList<>();
        for (TimelineLayer layer : layers) {
            if (!(layer instanceof Band))
                replacement.add(layer);
        }
        for (BandType bandType : bandTypes)
            addUnique(replacement, getOrCreateBand(bandType));
        replaceAll(replacement);
    }

    private void replaceAll(List<TimelineLayer> replacement) {
        for (TimelineLayer layer : layers) {
            if (!replacement.contains(layer))
                layer.remove();
        }
        layers.clear();
        layers.addAll(replacement);
        replacement.forEach(this::configureLayer);
        fireTableDataChanged();
        DrawController.layoutChanged();
        fetchData(DrawController.selectedAxis);
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
        return COLUMN_COUNT;
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

    private void configureLayer(TimelineLayer layer) {
        if (layer instanceof Band band)
            band.setOnAppearanceChanged(() -> updateCell(layers.indexOf(band), APPEARANCE_COLUMN));
    }

    private static void addUnique(List<TimelineLayer> target, TimelineLayer layer) {
        if (!containsLayer(target, layer))
            target.add(layer);
    }

    private static boolean containsLayer(List<TimelineLayer> searchLayers, TimelineLayer target) {
        if (target instanceof Band band)
            return findBand(searchLayers, band.getBandType()) != null;
        return searchLayers.contains(target);
    }

    @Nullable
    private static Band findBand(List<TimelineLayer> searchLayers, BandType bandType) {
        for (TimelineLayer layer : searchLayers) {
            if (layer instanceof Band band && band.getBandType().equals(bandType))
                return band;
        }
        return null;
    }

}
