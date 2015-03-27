package org.helioviewer.jhv.gui.components.layerTable;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.LayeredView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;

/**
 * A TableModel representing the state of visible Layers, internally using the
 * LayersModel
 *
 * @author Malte Nuhn
 *
 */
public class LayerTableModel extends AbstractTableModel implements LayersListener {

    private static final long serialVersionUID = 1167923521718778146L;

    public static final int COLUMN_VISIBILITY = 0;
    public static final int COLUMN_TITLE = 1;
    public static final int COLUMN_TIMESTAMP = 2;
    public static final int COLUMN_BUTTON_REMOVE = 3;

    /** The sole instance of this class. */
    private static final LayerTableModel layerTableModel = new LayerTableModel();

    private final List<JHVJP2View> views;

    /**
     * Returns the only instance of this class.
     *
     * @return the only instance of this class.
     * */
    public static LayerTableModel getSingletonInstance() {
        return layerTableModel;
    }

    private LayerTableModel() {
        views = new ArrayList<JHVJP2View>();
        LayersModel.getSingletonInstance().addLayersListener(this);
    }

    public void setVisible(int index, boolean visible) {
        if (index >= 0 && index < views.size()) {
            LayersModel.getSingletonInstance().setVisibleLink(views.get(index), visible);
        }
    }

    public boolean isVisible(int index) {
        if (index >= 0 && index < views.size()) {
            return LayersModel.getSingletonInstance().isVisible(views.get(index));
        }
        return false;
    }

    public void removeLayer(int index) {
        if (index >= 0 && index < views.size()) {
            LayersModel.getSingletonInstance().removeLayer(views.get(index));
        }
    }

    public View getViewAt(int index) {
        if (index >= 0 && index < views.size()) {
            return views.get(index);
        }
        return null;
    }

    public void moveLayerUp(int index) {
        if (index >= 0 && index < views.size()) {
            int newLevel = index;
            if (newLevel < views.size() - 1) {
                newLevel++;
            }
            LayeredView lv = LayersModel.getSingletonInstance().getLayeredView();
            if (lv != null) {
                lv.moveView(views.get(index), newLevel);
                LayersModel.getSingletonInstance().setActiveLayer(invertIndex(newLevel));
            }
        }
    }

    public void moveLayerDown(int index) {
        if (index >= 0 && index < views.size()) {
            int newLevel = index;
            if (newLevel > 0) {
                newLevel--;
            }
            LayeredView lv = LayersModel.getSingletonInstance().getLayeredView();
            if (lv != null) {
                lv.moveView(views.get(index), newLevel);
                LayersModel.getSingletonInstance().setActiveLayer(invertIndex(newLevel));
            }
        }
    }

    private int invertIndex(int idx) {
        // invert indices
        if (idx >= 0 && views.size() > 0) {
            idx = views.size() - 1 - idx;
        }
        return idx;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRowCount() {
        return views.size();
    }

    /**
     * {@inheritDoc} Hardcoded value of columns. This value is dependent on the
     * actual design of the LayerTable
     */
    @Override
    public int getColumnCount() {
        return 4;
    }

    /**
     * Return the LayerDescriptor for the given row of the table, regardless
     * which column is requested.
     */
    @Override
    public Object getValueAt(int row, int col) {
        int idx = row;
        if (idx >= 0 && idx < views.size()) {
            return LayersModel.getSingletonInstance().getDescriptor(views.get(idx));
        } else {
            return null;
        }

    }

    /**
     * Method part of the LayersListener interface, itself calling the
     * appropriate TableModel notification methods
     */
    @Override
    public void layerAdded(int newIndex) {
        updateData();
        fireTableRowsInserted(newIndex, newIndex);
    }

    public void layerChanged(int idx) {
        updateData();
        fireTableRowsUpdated(idx, idx);
    }

    /**
     * Method part of the LayersListener interface, itself calling the
     * appropriate TableModel notification methods
     */
    @Override
    public void layerRemoved(View oldView, int oldIndex) {
        updateData();
        fireTableRowsDeleted(oldIndex, oldIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void activeLayerChanged(View view) {
    }

    private void updateData() {
        LayeredView lv = LayersModel.getSingletonInstance().getLayeredView();
        views.clear();
        if (lv != null) {
            for (int i = lv.getNumLayers() - 1; i >= 0; i--) {
                views.add(lv.getLayer(i));
            }
        }
    }

}
