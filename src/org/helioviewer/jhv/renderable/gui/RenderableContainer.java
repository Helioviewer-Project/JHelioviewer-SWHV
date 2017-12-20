package org.helioviewer.jhv.renderable.gui;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.GridLayer;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.ImageLayers;
import org.helioviewer.jhv.layers.MiniviewLayer;
import org.helioviewer.jhv.layers.TimestampLayer;
import org.helioviewer.jhv.layers.ViewpointLayer;

import com.jogamp.opengl.GL2;

@SuppressWarnings("serial")
public class RenderableContainer extends AbstractTableModel implements Reorderable {

    private static class CompositeList extends AbstractList<Renderable> {

        private final ArrayList<ImageLayer> list1 = new ArrayList<>();
        private final ArrayList<Renderable> list2 = new ArrayList<>();
        final List<ImageLayer> imageLayers = Collections.unmodifiableList(list1);

        @Override
        public Renderable get(int index) {
            if (index < list1.size())
                return list1.get(index);
            return list2.get(index - list1.size());
        }

        @Override
        public Renderable remove(int index) {
            if (index < list1.size())
                return list1.remove(index);
            return list2.remove(index - list1.size());
        }

        @Override
        public int size() {
            return list1.size() + list2.size();
        }

        @Override
        public boolean add(Renderable e) {
            if (e instanceof ImageLayer)
                return list1.add((ImageLayer) e);
            return list2.add(e);
        }

        @Override
        public void add(int index, Renderable e) {
            if (!(e instanceof ImageLayer)) // only for DnD
                return;
            list1.add(index, (ImageLayer) e);
        }

    }

    private static CompositeList renderables = new CompositeList();
    private static CompositeList newRenderables = new CompositeList();
    private static final HashSet<Renderable> removedRenderables = new HashSet<>();

    private static GridLayer gridLayer;
    private static ViewpointLayer viewpointLayer;
    private static MiniviewLayer miniviewLayer;

    public RenderableContainer() {
        addRenderable(new GridLayer(null));
        addRenderable(new ViewpointLayer(null));
        addRenderable(new TimestampLayer(null));
        addRenderable(new MiniviewLayer(null));
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

    public void addRenderable(Renderable renderable) {
        renderables.add(renderable);
        newRenderables.add(renderable);

        if (renderable instanceof GridLayer)
            gridLayer = (GridLayer) renderable;
        else if (renderable instanceof ViewpointLayer)
            viewpointLayer = (ViewpointLayer) renderable;
        else if (renderable instanceof MiniviewLayer)
            miniviewLayer = (MiniviewLayer) renderable;

        int row = renderables.indexOf(renderable);
        fireTableRowsInserted(row, row);
        Displayer.display(); // e.g., PFSS renderable
    }

    public void removeRenderable(Renderable renderable) {
        int row = renderables.indexOf(renderable);
        renderables.remove(renderable);
        removedRenderables.add(renderable);
        if (row >= 0)
            fireTableRowsDeleted(row, row);
        Displayer.display();
    }

    public static void prerender(GL2 gl) {
        removeRenderables(gl);
        initRenderables(gl);
        for (Renderable renderable : renderables) {
            renderable.prerender(gl);
        }
    }

    public static void render(Camera camera, Viewport vp, GL2 gl) {
        for (Renderable renderable : renderables) {
            renderable.render(camera, vp, gl);
        }
    }

    public static void renderScale(Camera camera, Viewport vp, GL2 gl) {
        for (Renderable renderable : renderables) {
            renderable.renderScale(camera, vp, gl);
        }
    }

    public static void renderFloat(Camera camera, Viewport vp, GL2 gl) {
        for (Renderable renderable : renderables) {
            renderable.renderFloat(camera, vp, gl);
        }
    }

    public static void renderFullFloat(Camera camera, Viewport vp, GL2 gl) {
        for (Renderable renderable : renderables) {
            renderable.renderFullFloat(camera, vp, gl);
        }
    }

    public static void renderMiniview(Camera camera, Viewport miniview, GL2 gl) {
        MiniviewLayer.renderBackground(camera, miniview, gl);
        for (Renderable renderable : renderables) {
            renderable.renderMiniview(camera, miniview, gl);
        }
    }

    private static void initRenderables(GL2 gl) {
        for (Renderable renderable : newRenderables) {
            renderable.init(gl);
        }
        newRenderables.clear();
    }

    private static void removeRenderables(GL2 gl) {
        for (Renderable renderable : removedRenderables) {
            renderable.remove(gl);
        }
        removedRenderables.clear();
    }

    private static void insertRow(int row, Renderable rowData) {
        if (row > renderables.size()) {
            renderables.add(rowData);
        } else {
            renderables.add(row, rowData);
        }
    }

    @Override
    public void reorder(int fromIndex, int toIndex) {
        if (toIndex > renderables.size()) {
            return;
        }
        Renderable toMove = renderables.get(fromIndex);
        Renderable moveTo = renderables.get(Math.max(0, toIndex - 1));

        if (!(toMove instanceof ImageLayer) || !(moveTo instanceof ImageLayer)) {
            return;
        }
        renderables.remove(fromIndex);
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
        return renderables.size();
    }

    @Override
    public int getColumnCount() {
        return RenderableContainerPanel.NUMBER_COLUMNS;
    }

    @Override
    public Object getValueAt(int row, int col) {
        try {
            return renderables.get(row);
        } catch (Exception e) {
            return null;
        }
    }

    public void updateCell(int row, int col) {
        if (row >= 0) // negative row breaks model
            fireTableCellUpdated(row, col);
    }

    public void fireTimeUpdated(Renderable renderable) {
        updateCell(renderables.indexOf(renderable), RenderableContainerPanel.TIME_COL);
    }

    public static void dispose(GL2 gl) {
        for (Renderable renderable : renderables) {
            renderable.dispose(gl);
        }
        newRenderables = renderables;
        renderables = new CompositeList();
    }

    public /*temp*/ static List<ImageLayer> getImageLayers() {
        return renderables.imageLayers;
    }

    static List<Renderable> getRenderables() {
        return Collections.unmodifiableList(renderables);
    }

    static void removeAll() {
        removedRenderables.addAll(renderables);
        renderables = new CompositeList();
    }

}
