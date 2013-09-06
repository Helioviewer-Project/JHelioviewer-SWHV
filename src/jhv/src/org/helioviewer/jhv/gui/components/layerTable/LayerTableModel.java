package org.helioviewer.jhv.gui.components.layerTable;

import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.View;

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

    /**
     * Returns the only instance of this class.
     * 
     * @return the only instance of this class.
     * */
    public static LayerTableModel getSingletonInstance() {
        return layerTableModel;
    }

    private LayerTableModel() {
        LayersModel.getSingletonInstance().addLayersListener(this);
    }

    /**
     * {@inheritDoc}
     */
    public int getRowCount() {
        return LayersModel.getSingletonInstance().getNumLayers();
    }

    /**
     * {@inheritDoc} Hardcoded value of columns. This value is dependent on the
     * actual design of the LayerTable
     */
    public int getColumnCount() {
        return 4;
    }

    /**
     * Return the LayerDescriptor for the given row of the table, regardless
     * which column is requested.
     */
    public Object getValueAt(int row, int col) {

        int idx = row;

        return LayersModel.getSingletonInstance().getDescriptor(idx);

    }

    /**
     * Method part of the LayersListener interface, itself calling the
     * appropriate TableModel notification methods
     */
    public void layerAdded(final int newIndex) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                fireTableRowsInserted(newIndex, newIndex);
            }
        });
    }

    /**
     * Method part of the LayersListener interface, itself calling the
     * appropriate TableModel notification methods
     */
    public void layerChanged(final int idx) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                fireTableRowsUpdated(idx, idx);
            }
        });
    }

    /**
     * Method part of the LayersListener interface, itself calling the
     * appropriate TableModel notification methods
     */
    public void layerRemoved(View oldView, final int oldIndex) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                fireTableRowsDeleted(oldIndex, oldIndex);
            }
        });
    }

    /**
     * Method part of the LayersListener interface, itself calling the
     * appropriate TableModel notification methods
     */
    public void timestampChanged(final int idx) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                fireTableRowsUpdated(idx, idx);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public void activeLayerChanged(final int index) {
    }

    /**
     * {@inheritDoc}
     */
    public void viewportGeometryChanged() {
    }

    /**
     * {@inheritDoc}
     */
    public void subImageDataChanged() {
    }

    /**
     * {@inheritDoc}
     */
    public void layerDownloaded(int idx) {
    }
}
