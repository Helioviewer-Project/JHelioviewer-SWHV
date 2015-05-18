package org.helioviewer.jhv.plugin.renderable;

import java.util.ArrayList;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.renderable.RenderableImageType;

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
            if (renderables.get(i).getType() instanceof RenderableImageType) {
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

    public void render(GL2 gl) {
        removeRenderables(gl);
        initRenderables(gl);

        for (Renderable renderable : renderables) {
            renderable.render(gl);
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
        renderables.remove(fromIndex);
        if (fromIndex < toIndex) {
            this.insertRow(toIndex - 1, toMove);
        } else {
            this.insertRow(toIndex, toMove);
        }
        fireListeners();
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

    private void fireListeners() {
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

}
