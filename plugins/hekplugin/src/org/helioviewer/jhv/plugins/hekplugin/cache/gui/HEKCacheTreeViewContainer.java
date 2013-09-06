package org.helioviewer.jhv.plugins.hekplugin.cache.gui;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.helioviewer.jhv.gui.components.tristateCheckbox.TristateCheckBox;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKCache;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKCacheListener;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKCacheLoadingModel;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKPath;
import org.helioviewer.viewmodel.view.View;

/**
 * GUI Component showing either the HEKCacheTreeView, or a notice that currently
 * no event-structure exists.
 * 
 * The class updates/decides which component to show based on callback methods
 * called by the HEKCacheModel, by implementing the HEKCacheListener interface
 * 
 * @author Malte Nuhn
 */
public class HEKCacheTreeViewContainer extends JPanel implements HEKCacheListener, LayersListener {

    private static final long serialVersionUID = 1L;

    private static final String loadingStructureMessage = "Requesting Available Event Types";
    private static final String noInformationMessage = "No Event Information Available yet";

    boolean wasLoaded = false;

    private JLabel emptyLabel = new JLabel(noInformationMessage, JLabel.CENTER);

    CardLayout cl = new CardLayout();

    public HEKCacheTreeViewContainer() {
        super();
        this.setLayout(cl);
        HEKCache.getSingletonInstance().getModel().addCacheListener(this);
        LayersModel.getSingletonInstance().addLayersListener(this);

        emptyLabel.setFont(emptyLabel.getFont().deriveFont(Font.ITALIC));
        emptyLabel.setHorizontalTextPosition(JLabel.CENTER);
        emptyLabel.setOpaque(true);
        emptyLabel.setBackground(Color.WHITE);

        // Create the scroll pane and add the table to it.
        JScrollPane emptyScrollPane = new JScrollPane(emptyLabel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        emptyScrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        this.setEmpty(emptyScrollPane);

        update();
    }

    public void setMain(Component main) {
        this.add(main, "main");
    }

    private void setEmpty(Component empty) {
        this.add(empty, "empty");
    }

    /**
     * Sync the state of this component with the state of the LayersModel
     */
    public void update() {

        // root loading?
        boolean rootLoading = HEKCache.getSingletonInstance().getLoadingModel().getState(HEKCache.getSingletonInstance().getModel().getRoot(), false) != HEKCacheLoadingModel.PATH_NOTHING;
        // boolean anyLoading =
        // HEKCache.getSingletonInstance().getModel().isLoading();
        boolean layersAvailable = LayersModel.getSingletonInstance().getNumLayers() > 0;

        boolean show = !rootLoading && layersAvailable && wasLoaded;

        if (!layersAvailable && wasLoaded) {
            // reset the selection state
            HEKCache.getSingletonInstance().getController().setStates(TristateCheckBox.UNCHECKED);
        }
        if (layersAvailable) {
            wasLoaded = true;
        } else {
            wasLoaded = false;
        }

        if (rootLoading) {
            setLoadingMessage(loadingStructureMessage);
        } else if (!layersAvailable) {
            setLoadingMessage(noInformationMessage);
        }

        // setLoadingMessage("LayersAvailable: " + layersAvailable +
        // " - RootLoading: " + rootLoading + " - anyLoading: " + anyLoading);

        if (show) {
            cl.show(this, "main");
        } else {
            cl.show(this, "empty");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void cacheStateChanged() {
        update();
    }

    public void layerAdded(int idx) {
        update();
    }

    public void layerRemoved(View oldView, int oldIdx) {
        update();
    }

    public void layerChanged(int idx) {
    }

    public void activeLayerChanged(int idx) {
    }

    public void viewportGeometryChanged() {
    }

    public void timestampChanged(int idx) {
    }

    public void subImageDataChanged() {
    }

    public void setLoadingMessage(String msg) {
        emptyLabel.setText(msg);
    }

    public void eventsChanged(HEKPath path) {
    }

    public void structureChanged(HEKPath path) {
    }

    /**
     * {@inheritDoc}
     */
    public void layerDownloaded(int idx) {
    }
}
