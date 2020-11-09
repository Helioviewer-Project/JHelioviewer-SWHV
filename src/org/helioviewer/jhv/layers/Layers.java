package org.helioviewer.jhv.layers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.selector.Reorderable;
import org.helioviewer.jhv.layers.selector.LayersPanel;

import com.jogamp.opengl.GL2;

@SuppressWarnings({"serial", "unchecked"})
public class Layers extends AbstractTableModel implements Reorderable {

    private static class LayerList extends ArrayList<Layer> {

        int imageLayersCount;

        @Override
        public void clear() {
            super.clear();
            imageLayersCount = 0;
        }

        @Override
        public Layer remove(int index) {
            Layer ret = super.remove(index);
            if (ret instanceof ImageLayer)
                imageLayersCount--;
            return ret;
        }

        @Override
        public boolean remove(Object o) {
            boolean ret = super.remove(o);
            if (ret && o instanceof ImageLayer)
                imageLayersCount--;
            return ret;
        }

        @Override
        public boolean add(Layer e) {
            if (e instanceof ImageLayer)
                super.add(imageLayersCount++, e);
            else
                super.add(e);
            return true;
        }

        @Override
        public void add(int index, Layer e) { // only for DnD
            if (!(e instanceof ImageLayer))
                return;
            super.add(index, e);
            imageLayersCount++;
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

    private static LayerList layers = new LayerList();
    private static LayerList newLayers = new LayerList();
    private static final ArrayList<Layer> removedLayers = new ArrayList<>();

    private static GridLayer gridLayer;
    private static ViewpointLayer viewpointLayer;
    private static MiniviewLayer miniviewLayer;

    public Layers() {
        add(new ViewpointLayer(null));
        add(new GridLayer(null));
        add(new FOVLayer(null));
        add(new ConnectLayer(null));
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
            int count = layers.imageLayersCount;
            setActiveImageLayer(count == 0 ? null : (ImageLayer) layers.get(count - 1));
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
        layers = new LayerList();
    }

    public static void forEachImageLayer(Consumer<? super ImageLayer> action) {
        for (int i = 0; i < layers.imageLayersCount; i++)
            action.accept((ImageLayer) layers.get(i));
    }

    public static List<ImageLayer> getImageLayers() {
        return Collections.unmodifiableList((List<ImageLayer>) (Object) layers.subList(0, layers.imageLayersCount));
    }

    public static List<Layer> getLayers() {
        return Collections.unmodifiableList(layers);
    }

    public static void clear() {
        removedLayers.addAll(layers);
        layers = new LayerList();
        setActiveImageLayer(null);
    }

}
