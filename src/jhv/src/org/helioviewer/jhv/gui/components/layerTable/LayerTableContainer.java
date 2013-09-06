package org.helioviewer.jhv.gui.components.layerTable;

import java.awt.CardLayout;
import java.awt.Component;

import javax.swing.JPanel;

import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.View;

/**
 * GUI Component showing either the LayerTable, or a notice that currently no
 * layers exists.
 * 
 * The class updates/decides which component to show based on callback methods
 * called by the LayersModel, by implementing the LayersListener interface
 * 
 * @author Malte Nuhn
 */
public class LayerTableContainer extends JPanel implements LayersListener {

    private static final long serialVersionUID = 4954283312509735677L;

    CardLayout cl = new CardLayout();

    /**
     * Construct a new LayerTableContainer
     * 
     * @param table
     *            - the Component to show if at least one layer exists
     * @param empty
     *            - the Component to show if no layers exist
     */
    public LayerTableContainer(Component table, Component empty) {

        super();

        this.setLayout(cl);

        this.add(empty, "empty");
        this.add(table, "table");

        LayersModel.getSingletonInstance().addLayersListener(this);
        update();

    }

    /**
     * Sync the state of this component with the state of the LayersModel
     */
    public void update() {
        if (LayersModel.getSingletonInstance().getNumLayers() == 0) {
            cl.show(this, "empty");
        } else {
            cl.show(this, "table");
        }
    }

    /**
     * Sync the state of this component if a layer has been added or removed
     */
    public void layerAdded(int newIndex) {
        update();
    }

    /**
     * Sync the state of this component if a layer has been added or removed
     */
    public void layerRemoved(View oldView, int oldIndex) {
        update();
    }

    /**
     * {@inheritDoc}
     */
    public void activeLayerChanged(int index) {
    }

    /**
     * {@inheritDoc}
     */
    public void layerChanged(int index) {
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
    public void timestampChanged(int idx) {
    }

    /**
     * {@inheritDoc}
     */
    public void layerDownloaded(int idx) {
    }

}
