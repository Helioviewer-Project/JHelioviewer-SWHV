package org.helioviewer.jhv.renderable.gui;

import java.util.ArrayList;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.helioviewer.jhv.camera.GL3DViewport;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.renderable.components.RenderableImageLayer;
import org.helioviewer.jhv.renderable.components.RenderableMiniview;
import org.helioviewer.viewmodel.view.View;

import com.jogamp.opengl.GL2;

public class RenderableContainer implements TableModel, Reorderable {

    private final ArrayList<Renderable> renderables = new ArrayList<Renderable>();
    private final ArrayList<Renderable> newRenderables = new ArrayList<Renderable>();
    private final ArrayList<Renderable> removedRenderables = new ArrayList<Renderable>();
    private final ArrayList<TableModelListener> listeners = new ArrayList<TableModelListener>();

    public RenderableContainer() {
        super();
    }

    public void addBeforeRenderable(Renderable renderable) {
        int lastImagelayerIndex = -1;
        int size = renderables.size();
        for (int i = 0; i < size; i++) {
            if (renderables.get(i) instanceof RenderableImageLayer) {
                lastImagelayerIndex = i;
            }
        }
        renderables.add(lastImagelayerIndex + 1, renderable);
        newRenderables.add(renderable);
        fireListeners();
    }

    public void addRenderable(Renderable renderable) {
        renderables.add(renderable);
        newRenderables.add(renderable);
        fireListeners();
    }

    public void removeRenderable(Renderable renderable) {
        renderables.remove(renderable);
        removedRenderables.add(renderable);
        fireListeners();
        Displayer.display();
    }

    public void prerender(GL2 gl) {
        removeRenderables(gl);
        initRenderables(gl);

        for (Renderable renderable : renderables) {
            renderable.prerender(gl);
        }
    }

    public void render(GL2 gl, GL3DViewport vp) {

        for (Renderable renderable : renderables) {
            renderable.render(gl, vp);
        }
    }

    public void renderMiniview(GL2 gl, GL3DViewport miniview) {
        RenderableMiniview mv = ImageViewerGui.getRenderableMiniview();
        mv.renderMiniview(gl, miniview);
        for (Renderable renderable : renderables) {
            if (renderable != mv)
                renderable.renderMiniview(gl, miniview);
        }
    }

    private void initRenderables(GL2 gl) {
        for (Renderable renderable : newRenderables) {
            renderable.init(gl);
        }
        newRenderables.clear();
    }

    private void removeRenderables(GL2 gl) {
        for (Renderable renderable : removedRenderables) {
            renderable.remove(gl);
        }
        removedRenderables.clear();
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
        Renderable toMove = this.renderables.get(fromIndex);
        Renderable moveTo = this.renderables.get(Math.max(0, toIndex - 1));

        if (!(toMove instanceof RenderableImageLayer) || !(moveTo instanceof RenderableImageLayer)) {
            return;
        }
        renderables.remove(fromIndex);
        if (fromIndex < toIndex) {
            this.insertRow(toIndex - 1, toMove);
        } else {
            this.insertRow(toIndex, toMove);
        }
        fireListeners();
        arrangeMultiView(Displayer.multiview);

    }

    @Override
    public int getRowCount() {
        return this.renderables.size();
    }

    @Override
    public int getColumnCount() {
        return RenderableContainerPanel.NUMBEROFCOLUMNS;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return "" + columnIndex;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return Renderable.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return renderables.get(rowIndex);
    }

    public Renderable getTypedValueAt(int rowIndex, int columnIndex) {
        return renderables.get(rowIndex);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        renderables.add(rowIndex, (Renderable) aValue);
        fireListeners();
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        this.listeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        this.listeners.remove(l);
    }

    public void removeRow(int row) {
        Renderable el = this.renderables.get(row);
        this.removeRenderable(el);
    }

    public void fireListeners() {
        for (TableModelListener listener : this.listeners) {
            TableModelEvent e = new TableModelEvent(this);
            listener.tableChanged(e);
        }
    }

    public void fireTimeUpdated(Renderable renderable) {
        int idx = this.renderables.indexOf(renderable);
        if (idx < 0 || idx >= this.renderables.size())
            return;
        for (TableModelListener listener : this.listeners) {
            TableModelEvent e = new TableModelEvent(this, idx, idx, RenderableContainerPanel.TIMEROW, TableModelEvent.UPDATE);
            listener.tableChanged(e);
        }
    }

    public int getRowIndex(Renderable renderable) {
        return this.renderables.indexOf(renderable);
    }

    public void init(GL2 gl) {
        for (Renderable renderable : this.renderables) {
            renderable.init(gl);
        }
    }

    public void dispose(GL2 gl) {
        for (Renderable renderable : this.renderables) {
            renderable.dispose(gl);
        }
    }

    public boolean isViewportActive(int idx) {
        for (Renderable renderable : this.renderables) {
            if (renderable instanceof RenderableImageLayer && renderable.isVisible(idx))
                return true;
        }
        return false;
    }

    public void arrangeMultiView(boolean multiview) {
        int ctImages = 0;

        if (multiview) {
            for (Renderable r : this.renderables) {
                if (r instanceof RenderableImageLayer && r.isVisible()) {
                    RenderableImageLayer im = (RenderableImageLayer) r;
                    r.setVisible(ctImages);
                    im.getglImage().setOpacity(1f);
                    ctImages++;
                }
            }
        } else {
            for (Renderable r : this.renderables) {
                if (r instanceof RenderableImageLayer && r.isVisible()) {
                    RenderableImageLayer im = (RenderableImageLayer) r;
                    View view = im.getView();
                    r.setVisible(0);
                    float opacity;
                    if (view.getName().contains("LASCO") || view.getName().contains("COR"))
                        opacity = 1.f;
                    else {
                        opacity = (float) (1. / (1. + ctImages));
                        ctImages++;
                    }
                    im.getglImage().setOpacity(opacity);
                    ctImages++;
                }
            }
        }
        for (GL3DViewport vp : Displayer.getViewports()) {
            vp.computeActive();
        }
        Displayer.reshapeAll();
        Displayer.getViewport().getCamera().updateCameraWidthAspect(Displayer.getGLWidth() / (double) Displayer.getGLHeight());
        Displayer.render();
    }
}
