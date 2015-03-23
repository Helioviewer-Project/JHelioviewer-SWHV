package org.helioviewer.jhv.plugin.renderable;

import java.util.ArrayList;

import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.jhv.renderable.RenderableGrid;
import org.helioviewer.jhv.renderable.RenderableSolarAxes;
import org.helioviewer.jhv.renderable.RenderableSolarAxesType;

public class RenderableContainer extends DefaultTableModel {
    private final ArrayList<Renderable> renderables = new ArrayList<Renderable>();
    private final ArrayList<Renderable> newRenderables = new ArrayList<Renderable>();
    private final ArrayList<Renderable> removedRenderables = new ArrayList<Renderable>();
    private final ArrayList<TableModelListener> listeners = new ArrayList<TableModelListener>();

    public RenderableContainer() {
        super();

        RenderableSolarAxesType solarAxesType = new RenderableSolarAxesType("Solar Axes");
        addRenderable(new RenderableSolarAxes(solarAxesType));
        RenderableSolarAxesType gridType = new RenderableSolarAxesType("Grids");
        addRenderable(new RenderableGrid(gridType, 20, 20, false));

    }

    public void addRenderable(Renderable renderable) {
        newRenderables.add(renderable);
    }

    public void removeRenderable(Renderable renderable) {
        renderables.remove(renderable);
        removedRenderables.add(renderable);
    }

    public void render(GL3DState state) {
        initRenderables(state);
        removeRenderables(state);
        for (Renderable renderable : renderables) {
            renderable.render(state);
        }
    }

    private void initRenderables(GL3DState state) {
        for (Renderable renderable : newRenderables) {
            renderable.init(state);
            renderables.add(renderable);
        }
        newRenderables.clear();
    }

    private void removeRenderables(GL3DState state) {
        for (Renderable renderable : removedRenderables) {
            renderable.remove(state);
        }
    }

    @Override
    public int getRowCount() {
        if (renderables != null)
            return renderables.size();
        else
            return 0;
    }

    @Override
    public int getColumnCount() {
        return RenderableContainerPanel.NUMBEROFCOLUMNS;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return "DC";
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return Renderable.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return renderables.get(rowIndex);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        renderables.set(rowIndex, (Renderable) aValue);
    }

    @Override
    public void removeRow(int row) {
        renderables.remove(row);
        fireTableRowsDeleted(row, row);
    }

}
