package org.helioviewer.jhv.layers;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.selector.Reorderable;
import org.helioviewer.jhv.layers.selector.LayersPanel;

import com.jogamp.opengl.GL2;

@SuppressWarnings("serial")
public class Layers extends AbstractTableModel implements Reorderable {

    private static class CompositeList extends AbstractList<Layer> {

        private final ArrayList<ImageLayer> list1 = new ArrayList<>();
        private final ArrayList<Layer> list2 = new ArrayList<>();
        final List<ImageLayer> imageLayers = Collections.unmodifiableList(list1);

        @Override
        public Layer get(int index) {
            int size = list1.size();
            if (index < size)
                return list1.get(index);
            return list2.get(index - size);
        }

        @Override
        public Layer remove(int index) {
            int size = list1.size();
            if (index < size) {
                return list1.remove(index);
            }
            return list2.remove(index - size);
        }

        @Override
        public int size() {
            return list1.size() + list2.size();
        }

        @Override
        public boolean add(Layer e) {
            if (e instanceof ImageLayer) {
                return list1.add((ImageLayer) e);
            }
            return list2.add(e);
        }

        @Override
        public void add(int index, Layer e) {
            if (!(e instanceof ImageLayer)) // only for DnD
                return;
            list1.add(index, (ImageLayer) e);
        }

    }

    private static ImageLayer activeLayer;

    public static ImageLayer getActiveImageLayer() {
        return activeLayer;
    }

    public static void setActiveImageLayer(ImageLayer layer) {
        activeLayer = layer;
        Movie.setMaster(activeLayer);
    }

    private static CompositeList layers = new CompositeList();
    private static CompositeList newLayers = new CompositeList();
    private static final HashSet<Layer> removedLayers = new HashSet<>();

    private static GridLayer gridLayer;
    private static ViewpointLayer viewpointLayer;
    private static MiniviewLayer miniviewLayer;

    public Layers() {
        add(new ViewpointLayer(null));
        add(new GridLayer(null));
        add(new TimestampLayer(null));
        add(new MiniviewLayer(null));
    }

    public static GridLayer getGridLayer() {
        return gridLayer;
    }

    public static ViewpointLayer getViewpointLayer() {
        return viewpointLayer;
    }

    public static MiniviewLayer getMiniviewLayer() {
        return miniviewLayer;
    }

    public void add(Layer layer) {
        layers.add(layer);
        newLayers.add(layer);

        if (layer instanceof GridLayer)
            gridLayer = (GridLayer) layer;
        else if (layer instanceof ViewpointLayer)
            viewpointLayer = (ViewpointLayer) layer;
        else if (layer instanceof MiniviewLayer)
            miniviewLayer = (MiniviewLayer) layer;

        int row = layers.indexOf(layer);
        fireTableRowsInserted(row, row);
        MovieDisplay.display(); // e.g., PFSS layer
    }

    public void remove(Layer layer) {
        int row = layers.indexOf(layer);
        layers.remove(layer);
        removedLayers.add(layer);

        if (layer == activeLayer) {
            int size = layers.imageLayers.size();
            setActiveImageLayer(size == 0 ? null : layers.imageLayers.get(size - 1));
        }

        if (row >= 0)
            fireTableRowsDeleted(row, row);
        MovieDisplay.display();
    }

    public static void prerender(GL2 gl) {
        removeLayers(gl);
        initLayers(gl);
        layers.forEach(layer -> layer.prerender(gl));
    }

    public static void render(Camera camera, Viewport vp, GL2 gl) {
        layers.forEach(layer -> layer.render(camera, vp, gl));
    }

    public static void renderScale(Camera camera, Viewport vp, GL2 gl) {
        layers.forEach(layer -> layer.renderScale(camera, vp, gl));
    }

    public static void renderFloat(Camera camera, Viewport vp, GL2 gl) {
        layers.forEach(layer -> layer.renderFloat(camera, vp, gl));
    }

    public static void renderFullFloat(Camera camera, Viewport vp, GL2 gl) {
        layers.forEach(layer -> layer.renderFullFloat(camera, vp, gl));
    }

    public static void renderMiniview(Camera camera, Viewport miniview, GL2 gl) {
        layers.forEach(layer -> layer.renderMiniview(camera, miniview, gl));
    }

    private static void initLayers(GL2 gl) {
        newLayers.forEach(layer -> layer.init(gl));
        newLayers.clear();
    }

    private static void removeLayers(GL2 gl) {
        removedLayers.forEach(layer -> layer.remove(gl));
        removedLayers.clear();
    }

    private static void insertRow(int row, Layer rowData) {
        if (row > layers.size()) {
            layers.add(rowData);
        } else {
            layers.add(row, rowData);
        }
    }

    @Override
    public void reorder(int fromIndex, int toIndex) {
        if (toIndex > layers.size()) {
            return;
        }
        Layer toMove = layers.get(fromIndex);
        Layer moveTo = layers.get(Math.max(0, toIndex - 1));

        if (!(toMove instanceof ImageLayer) || !(moveTo instanceof ImageLayer)) {
            return;
        }
        layers.remove(fromIndex);
        if (fromIndex < toIndex) {
            insertRow(toIndex - 1, toMove);
        } else {
            insertRow(toIndex, toMove);
        }

        if (Display.multiview) {
            ImageLayers.arrangeMultiView(true);
        }
    }

    @Override
    public int getRowCount() {
        return layers.size();
    }

    @Override
    public int getColumnCount() {
        return LayersPanel.NUMBER_COLUMNS;
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

    public void updateCell(int row, int col) {
        if (row >= 0) // negative row breaks model
            fireTableCellUpdated(row, col);
    }

    public void fireTimeUpdated(Layer layer) {
        updateCell(layers.indexOf(layer), LayersPanel.TIME_COL);
    }

    public static void dispose(GL2 gl) {
        layers.forEach(layer -> layer.dispose(gl));
        newLayers = layers;
        layers = new CompositeList();
    }

    public static List<ImageLayer> getImageLayers() {
        return layers.imageLayers;
    }

    public static List<Layer> getLayers() {
        return Collections.unmodifiableList(layers);
    }

    public static void clear() {
        removedLayers.addAll(layers);
        layers = new CompositeList();
        setActiveImageLayer(null);
    }

}
