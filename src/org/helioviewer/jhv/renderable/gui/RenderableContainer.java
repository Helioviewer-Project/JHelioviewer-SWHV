package org.helioviewer.jhv.renderable.gui;

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

    private ArrayList<Renderable> renderables = new ArrayList<>();
    private ArrayList<Renderable> newRenderables = new ArrayList<>();
    private final HashSet<Renderable> removedRenderables = new HashSet<>();

    private RenderableGrid renderableGrid;
    private RenderableViewpoint renderableViewpoint;
    private RenderableMiniview renderableMiniview;

    public RenderableContainer() {
        addRenderable(new RenderableGrid(null));
        addRenderable(new RenderableViewpoint(null));
        addRenderable(new RenderableTimeStamp(null));
        addRenderable(new RenderableMiniview(null));
    }

    public RenderableViewpoint getRenderableViewpoint() {
        return renderableViewpoint;
    }

    public RenderableGrid getRenderableGrid() {
        return renderableGrid;
    }

    public RenderableMiniview getRenderableMiniview() {
        return renderableMiniview;
    }

    public void addBeforeRenderable(Renderable renderable) {
        int lastImagelayerIndex = -1;
        int size = renderables.size();
        for (int i = 0; i < size; i++) {
            if (renderables.get(i) instanceof ImageLayer) {
                lastImagelayerIndex = i;
            }
        }
        renderables.add(lastImagelayerIndex + 1, renderable);
        newRenderables.add(renderable);

        int row = lastImagelayerIndex + 1;
        fireTableRowsInserted(row, row);
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

        int row = renderables.size() - 1;
        fireTableRowsInserted(row, row);
        Displayer.display(); // e.g., PFSS renderable
    }

    public void removeRenderable(Renderable renderable) {
        renderables.remove(renderable);
        removedRenderables.add(renderable);
        Displayer.display();
    }

    public void prerender(GL2 gl) {
        int count = removeRenderables(gl);
        initRenderables(gl);
        for (Renderable renderable : renderables) {
            renderable.prerender(gl);
        }
    }

    public void render(Camera camera, Viewport vp, GL2 gl) {
        for (Renderable renderable : renderables) {
            renderable.render(camera, vp, gl);
        }
    }

    public void renderScale(Camera camera, Viewport vp, GL2 gl) {
        for (Renderable renderable : renderables) {
            renderable.renderScale(camera, vp, gl);
        }
    }

    public void renderFloat(Camera camera, Viewport vp, GL2 gl) {
        for (Renderable renderable : renderables) {
            renderable.renderFloat(camera, vp, gl);
        }
    }

    public void renderFullFloat(Camera camera, Viewport vp, GL2 gl) {
        for (Renderable renderable : renderables) {
            renderable.renderFullFloat(camera, vp, gl);
        }
    }

    public void renderMiniview(Camera camera, Viewport miniview, GL2 gl) {
        RenderableMiniview.renderBackground(camera, miniview, gl);
        for (Renderable renderable : renderables) {
            renderable.renderMiniview(camera, miniview, gl);
        }
    }

    private void initRenderables(GL2 gl) {
        for (Renderable renderable : newRenderables) {
            renderable.init(gl);
        }
        newRenderables.clear();
    }

    private int removeRenderables(GL2 gl) {
        int count = removedRenderables.size();
        for (Renderable renderable : removedRenderables) {
            renderable.remove(gl);
        }
        removedRenderables.clear();
        return count;
    }

    private void insertRow(int row, Renderable rowData) {
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

    public void arrangeMultiView(boolean multiview) {
        if (multiview) {
            int ct = 0;
            for (Renderable r : renderables) {
                if (r instanceof ImageLayer) {
                    ImageLayer l = (ImageLayer) r;
                    if (l.isEnabled()) {
                        l.setVisible(ct);
                        ct++;
                    }
                }
            }
        } else {
            for (Renderable r : renderables) {
                if (r instanceof ImageLayer) {
                    ImageLayer l = (ImageLayer) r;
                    if (l.isEnabled()) {
                        l.setVisible(0);
                    }
                }
            }
        }
        Displayer.reshapeAll();
        Displayer.render(1);
    }

    public ImageLayer getImageLayerInViewport(int idx) {
        for (Renderable r : renderables) {
            if (r instanceof ImageLayer && r.isVisible(idx))
                return (ImageLayer) r;
        }
        return null;
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

    public void dispose(GL2 gl) {
        for (Renderable renderable : renderables) {
            renderable.dispose(gl);
        }
        newRenderables = renderables;
        renderables = new ArrayList<>();
    }

    public List<Renderable> getRenderables() {
        return Collections.unmodifiableList(renderables);
    }

    public void removeAll() {
        removedRenderables.addAll(renderables);
        renderables = new ArrayList<>();
    }

}
