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
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.renderable.components.RenderableGrid;
import org.helioviewer.jhv.renderable.components.RenderableMiniview;
import org.helioviewer.jhv.renderable.components.RenderableTimeStamp;
import org.helioviewer.jhv.renderable.components.RenderableViewpoint;

import com.jogamp.opengl.GL2;

@SuppressWarnings("serial")
public class RenderableContainer extends AbstractTableModel implements Reorderable {

    private static class CompositeList extends AbstractList<Renderable> {

        private final ArrayList<ImageLayer> list1 = new ArrayList<>();
        private final ArrayList<Renderable> list2 = new ArrayList<>();

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

        List<ImageLayer> getImageLayers() {
            return list1;
        }

    }

    private static CompositeList renderables = new CompositeList();
    private static CompositeList newRenderables = new CompositeList();
    private static final HashSet<Renderable> removedRenderables = new HashSet<>();

    private static RenderableGrid renderableGrid;
    private static RenderableViewpoint renderableViewpoint;
    private static RenderableMiniview renderableMiniview;

    public RenderableContainer() {
        addRenderable(new RenderableGrid(null));
        addRenderable(new RenderableViewpoint(null));
        addRenderable(new RenderableTimeStamp(null));
        addRenderable(new RenderableMiniview(null));
    }

    public static RenderableViewpoint getRenderableViewpoint() {
        return renderableViewpoint;
    }

    public static RenderableGrid getRenderableGrid() {
        return renderableGrid;
    }

    public static RenderableMiniview getRenderableMiniview() {
        return renderableMiniview;
    }

    public void addRenderable(Renderable renderable) {
        renderables.add(renderable);
        newRenderables.add(renderable);

        if (renderable instanceof RenderableGrid)
            renderableGrid = (RenderableGrid) renderable;
        else if (renderable instanceof RenderableViewpoint)
            renderableViewpoint = (RenderableViewpoint) renderable;
        else if (renderable instanceof RenderableMiniview)
            renderableMiniview = (RenderableMiniview) renderable;

        int row = renderables.indexOf(renderable);
        fireTableRowsInserted(row, row);
        Displayer.display(); // e.g., PFSS renderable
    }

    public static void removeRenderable(Renderable renderable) {
        renderables.remove(renderable);
        removedRenderables.add(renderable);
        Displayer.display();
    }

    public static void prerender(GL2 gl) {
        int count = removeRenderables(gl);
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
        RenderableMiniview.renderBackground(camera, miniview, gl);
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

    private static int removeRenderables(GL2 gl) {
        int count = removedRenderables.size();
        for (Renderable renderable : removedRenderables) {
            renderable.remove(gl);
        }
        removedRenderables.clear();
        return count;
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
            arrangeMultiView(true);
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

    static List<Renderable> getRenderables() {
        return Collections.unmodifiableList(renderables);
    }

    static void removeAll() {
        removedRenderables.addAll(renderables);
        renderables = new CompositeList();
    }

    public static void arrangeMultiView(boolean multiview) {
        List<ImageLayer> layers = renderables.getImageLayers();
        if (multiview) {
            int ct = 0;
            for (ImageLayer layer : layers) {
                if (layer.isEnabled()) {
                    layer.setVisible(ct);
                    ct++;
                }
            }
        } else {
            for (ImageLayer layer : layers) {
                if (layer.isEnabled())
                    layer.setVisible(0);
            }
        }
        Displayer.reshapeAll();
        Displayer.render(1);
    }

    public static ImageLayer getImageLayerInViewport(int idx) {
        for (ImageLayer layer : renderables.getImageLayers()) {
            if (layer.isVisible(idx))
                return layer;
        }
        return null;
    }

    public static int getNumEnabledImageLayers() {
        int ct = 0;
        for (ImageLayer layer : renderables.getImageLayers()) {
            if (layer.isEnabled())
                ct++;
        }
        return ct;
    }

}
