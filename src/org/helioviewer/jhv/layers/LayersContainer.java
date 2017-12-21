package org.helioviewer.jhv.layers;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.layers.selector.Reorderable;
import org.helioviewer.jhv.layers.selector.LayersPanel;
import org.helioviewer.jhv.view.View;

import com.jogamp.opengl.GL2;

// to be merged with Layers
@SuppressWarnings("serial")
public class LayersContainer extends AbstractTableModel implements Reorderable {

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
                if (activeLayer == list1.get(index))
                    setActiveImageLayer(size - 1 < 0 ? null : list1.get(size - 1));
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

    static void setMasterMovie(ImageLayer layer) {
        View view;
        if (layer == null || !(view = layer.getView()).isMultiFrame()) {
            Layers.pauseMovie();
            MoviePanel.unsetMovie();
        } else
            MoviePanel.setMovie(view);
    }

    public static ImageLayer getActiveImageLayer() {
        return activeLayer;
    }

    public static void setActiveImageLayer(ImageLayer layer) {
        if (layer != activeLayer) {
            activeLayer = layer;
            setMasterMovie(activeLayer);
        }
    }

    private static CompositeList layers = new CompositeList();
    private static CompositeList newLayers = new CompositeList();
    private static final HashSet<Layer> removedLayers = new HashSet<>();

    private static GridLayer gridLayer;
    private static ViewpointLayer viewpointLayer;
    private static MiniviewLayer miniviewLayer;

    public LayersContainer() {
        addLayer(new GridLayer(null));
        addLayer(new ViewpointLayer(null));
        addLayer(new TimestampLayer(null));
        addLayer(new MiniviewLayer(null));
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

    public void addLayer(Layer layer) {
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
        Displayer.display(); // e.g., PFSS layer
    }

    public void removeLayer(Layer layer) {
        int row = layers.indexOf(layer);
        layers.remove(layer);
        removedLayers.add(layer);
        if (row >= 0)
            fireTableRowsDeleted(row, row);
        Displayer.display();
    }

    public static void prerender(GL2 gl) {
        removeLayers(gl);
        initLayers(gl);
        for (Layer layer : layers) {
            layer.prerender(gl);
        }
    }

    public static void render(Camera camera, Viewport vp, GL2 gl) {
        for (Layer layer : layers) {
            layer.render(camera, vp, gl);
        }
    }

    public static void renderScale(Camera camera, Viewport vp, GL2 gl) {
        for (Layer layer : layers) {
            layer.renderScale(camera, vp, gl);
        }
    }

    public static void renderFloat(Camera camera, Viewport vp, GL2 gl) {
        for (Layer layer : layers) {
            layer.renderFloat(camera, vp, gl);
        }
    }

    public static void renderFullFloat(Camera camera, Viewport vp, GL2 gl) {
        for (Layer layer : layers) {
            layer.renderFullFloat(camera, vp, gl);
        }
    }

    public static void renderMiniview(Camera camera, Viewport miniview, GL2 gl) {
        MiniviewLayer.renderBackground(camera, miniview, gl);
        for (Layer layer : layers) {
            layer.renderMiniview(camera, miniview, gl);
        }
    }

    private static void initLayers(GL2 gl) {
        for (Layer layer : newLayers) {
            layer.init(gl);
        }
        newLayers.clear();
    }

    private static void removeLayers(GL2 gl) {
        for (Layer layer : removedLayers) {
            layer.remove(gl);
        }
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

        if (Displayer.multiview) {
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
        for (Layer layer : layers) {
            layer.dispose(gl);
        }
        newLayers = layers;
        layers = new CompositeList();
    }

    public static List<ImageLayer> getImageLayers() {
        return layers.imageLayers;
    }

    public static List<Layer> getLayers() {
        return Collections.unmodifiableList(layers);
    }

    public static void removeAll() {
        removedLayers.addAll(layers);
        layers = new CompositeList();
    }

}
