package org.helioviewer.jhv.plugin.renderable;

import java.util.ArrayList;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.jhv.renderable.RenderableGrid;
import org.helioviewer.jhv.renderable.RenderableSolarAxes;
import org.helioviewer.jhv.renderable.RenderableSolarAxesType;

public class RenderableContainer implements TableModel, Reorderable {
    private final ArrayList<Renderable> renderables = new ArrayList<Renderable>();
    private final ArrayList<Renderable> newRenderables = new ArrayList<Renderable>();
    private final ArrayList<Renderable> removedRenderables = new ArrayList<Renderable>();
    private final ArrayList<TableModelListener> listeners = new ArrayList<TableModelListener>();

    public RenderableContainer() {
        super();

        RenderableSolarAxesType solarAxesType = new RenderableSolarAxesType("Solar Axes");
        addRenderable(new RenderableSolarAxes(solarAxesType));
        RenderableSolarAxesType gridType = new RenderableSolarAxesType("Grids");
        addRenderable(new RenderableGrid(gridType, false));

    }

    public void addBeforeRenderable(Renderable renderable) {
        renderables.add(0, renderable);
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
    }

    public void render(GL3DState state) {
        removeRenderables(state);
        initRenderables(state);
        for (Renderable renderable : renderables) {
            renderable.render(state);
        }
    }

    private void initRenderables(GL3DState state) {
        for (Renderable renderable : newRenderables) {
            renderable.init(state);
        }
        newRenderables.clear();
    }

    private void removeRenderables(GL3DState state) {
        for (Renderable renderable : removedRenderables) {
            renderable.remove(state);
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
        this.renderables.remove(row);
        fireListeners();
    }

    private void fireListeners() {
        for (TableModelListener listener : this.listeners) {
            TableModelEvent e = new TableModelEvent(this);
            listener.tableChanged(e);
        }
    }

    public void fireTimeUpdated() {
        for (TableModelListener listener : this.listeners) {
            TableModelEvent e = new TableModelEvent(this);
            listener.tableChanged(e);
        }
    }
}
