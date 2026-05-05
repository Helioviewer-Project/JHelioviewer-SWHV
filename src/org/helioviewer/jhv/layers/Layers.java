package org.helioviewer.jhv.layers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.imagedata.ImageBufferCache;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeListener;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.view.NullView;

@SuppressWarnings("unchecked")
public final class Layers {

    @SuppressWarnings("serial")
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

    interface Listener {
        void rowsInserted(int firstRow, int lastRow);

        void rowsDeleted(int firstRow, int lastRow);

        void cellUpdated(int row, int col);

        void timeUpdated(Layer layer);
    }

    private static final ArrayList<Listener> listeners = new ArrayList<>();

    private static final NullImageLayer nullImageLayer = new NullImageLayer(NullView.create(TimeUtils.START.milli - 2 * TimeUtils.DAY_IN_MILLIS, TimeUtils.START.milli,
            TimeUtils.defaultCadence(TimeUtils.START.milli - 2 * TimeUtils.DAY_IN_MILLIS, TimeUtils.START.milli)));
    private static ImageLayer activeLayer = nullImageLayer;

    public static final TimeListener.Selection timeSelectionListener = Layers::timeSelectionChanged;

    private static void timeSelectionChanged(long start, long end) {
        nullImageLayer.setView(NullView.create(start, end, TimeUtils.defaultCadence(start, end)));
        // Replacing the placeholder NullView also needs a full Movie resync when it is active.
        if (activeLayer == nullImageLayer)
            Movie.setMaster(activeLayer);
        else
            Movie.timeRangeChanged();
    }

    public static ImageLayer getActiveImageLayer() {
        return activeLayer;
    }

    public static void setActiveImageLayer(ImageLayer layer) {
        activeLayer = layer == null ? nullImageLayer : layer;
        Movie.setMaster(activeLayer);
    }

    private static LayerList layers = new LayerList();
    private static LayerList newLayers = new LayerList();
    private static final ArrayList<Layer> removedLayers = new ArrayList<>();

    private static ViewpointLayer viewpointLayer;
    private static MiniviewLayer miniviewLayer;
    private static ConnectionLayer connectionLayer;

    static {
        add(new ViewpointLayer(null));
        add(new ConnectionLayer(null));
        add(new GridLayer(null));
        add(new FOVLayer(null));
        // add(new StarLayer(null));
        add(new TimestampLayer(null));
        add(new MiniviewLayer(null));
    }

    public static ViewpointLayer getViewpointLayer() {
        return viewpointLayer;
    }

    public static MiniviewLayer getMiniviewLayer() {
        return miniviewLayer;
    }

    public static ConnectionLayer getConnectionLayer() {
        return connectionLayer;
    }

    public static void add(Layer layer) {
        layers.add(layer);
        newLayers.add(layer);

        if (layer instanceof ViewpointLayer vl)
            viewpointLayer = vl;
        else if (layer instanceof MiniviewLayer ml)
            miniviewLayer = ml;
        else if (layer instanceof ConnectionLayer cl)
            connectionLayer = cl;

        int row = layers.indexOf(layer);
        listeners.forEach(listener -> listener.rowsInserted(row, row));
        MovieDisplay.display(); // e.g., PFSS layer
    }

    public static void remove(Layer layer) {
        int row = layers.indexOf(layer);
        if (row < 0)
            return;

        layers.remove(row);
        newLayers.remove(layer);
        removedLayers.add(layer);

        if (layer == activeLayer) {
            int count = layers.imageLayersCount;
            setActiveImageLayer(count == 0 ? null : (ImageLayer) layers.get(count - 1));
        }

        listeners.forEach(listener -> listener.rowsDeleted(row, row));
        MovieDisplay.display();
    }

    public static void prerender() {
        removeLayers();
        initLayers();
        layers.forEach(Layer::prerender);
        reapImageBuffers();
    }

    public static void render(Camera camera, Viewport vp) {
        layers.forEach(layer -> layer.render(camera, vp));
    }

    public static void renderScale(Camera camera, Viewport vp) {
        layers.forEach(layer -> layer.renderScale(camera, vp));
    }

    public static void renderFloat(Camera camera, Viewport vp) {
        layers.forEach(layer -> layer.renderFloat(camera, vp));
    }

    public static void renderFullFloat(Camera camera, Viewport vp) {
        layers.forEach(layer -> layer.renderFullFloat(camera, vp));
    }

    public static void renderMiniview(Camera camera, Viewport miniview) {
        layers.forEach(layer -> layer.renderMiniview(camera, miniview));
    }

    private static void initLayers() {
        newLayers.forEach(Layer::init);
        newLayers.clear();
    }

    private static void removeLayers() {
        removedLayers.forEach(Layer::remove);
        removedLayers.clear();
    }

    static void insertRow(int row, Layer rowData) {
        if (row > layers.size()) {
            layers.add(rowData);
        } else {
            layers.add(row, rowData);
        }
    }

    static void reorder(int fromIndex, int toIndex) {
        if (toIndex > layers.size()) {
            return;
        }
        Layer toMove = layers.get(fromIndex);
        if (!(toMove instanceof ImageLayer)) {
            return;
        }

        int target = Math.clamp(toIndex, 0, layers.imageLayersCount);
        if (fromIndex < target)
            target--; // adjust insertion index after removal
        if (fromIndex == target)
            return;

        layers.remove(fromIndex);
        insertRow(target, toMove);

        if (Display.multiview) {
            ImageLayers.arrangeMultiView(true);
        }
    }

    static int getRowCount() {
        return layers.size();
    }

    static Object getValueAt(int row) {
        try {
            return layers.get(row);
        } catch (Exception e) {
            return null;
        }
    }

    static void updateCell(int row, int col) {
        if (row >= 0) // negative row breaks model
            listeners.forEach(listener -> listener.cellUpdated(row, col));
    }

    public static void fireTimeUpdated(Layer layer) {
        listeners.forEach(listener -> listener.timeUpdated(layer));
    }

    static void addListener(Listener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    public static void dispose() {
        removeLayers();
        layers.forEach(Layer::dispose);
        newLayers.clear();
        newLayers.addAll(layers);
    }

    public static void remove() {
        newLayers = layers;
        layers = new LayerList();
        setActiveImageLayer(null);
    }

    public static void forEachImageLayer(Consumer<? super ImageLayer> action) {
        for (int i = 0; i < layers.imageLayersCount; i++)
            action.accept((ImageLayer) layers.get(i));
    }

    private static void reapImageBuffers() {
        Set<ImageBuffer> retained = Collections.newSetFromMap(new IdentityHashMap<>());
        forEachImageLayer(layer -> layer.collectImageBuffers(retained));
        ImageBufferCache.reap(retained);
    }

    public static void setImageLayersNearestFrame(JHVTime dateTime) {
        if (layers.imageLayersCount == 0)
            nullImageLayer.getView().setNearestFrame(dateTime);
        else
            forEachImageLayer(layer -> layer.getView().setNearestFrame(dateTime));
    }

    public static List<ImageLayer> getImageLayers() {
        return Collections.unmodifiableList((List<ImageLayer>) (Object) layers.subList(0, layers.imageLayersCount));
    }

    public static List<Layer> getLayers() {
        return Collections.unmodifiableList(layers);
    }

    public static void clear() {
        removedLayers.addAll(layers);
        newLayers.clear();
        layers = new LayerList();
        setActiveImageLayer(null);
    }

    private Layers() {}

}
