package org.helioviewer.jhv.gui.components;

import java.awt.CardLayout;
import java.awt.Component;
import java.util.HashMap;

import javax.swing.JPanel;

import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.AbstractView;
import org.helioviewer.viewmodel.view.View;

public class ControlPanelContainer extends JPanel implements LayersListener {

    HashMap<View, Component> controlMap = new HashMap<View, Component>();

    public ControlPanelContainer() {
        this.setLayout(new CardLayout());
        LayersModel.addLayersListener(this);
    }

    public void addLayer(View v, Component controlPanel) {
        controlMap.put(v, controlPanel);
        this.add(controlPanel, v.toString());
    }

    public void removeLayer(View v) {
        Component toRemove = controlMap.get(v);
        this.getLayout().removeLayoutComponent(toRemove);
    }

    public Component getViewComponent(View v) {
        return controlMap.get(v);
    }

    private void updateActiveView(AbstractView v) {
        CardLayout cl = (CardLayout) this.getLayout();
        cl.show(this, v == null ? "null" : v.toString());
        ensureSize();
    }

    public void ensureSize() {
        for (Component comp : this.getComponents()) {
            if (comp.isVisible()) {
                this.setPreferredSize(comp.getPreferredSize());
            }
        }
        revalidate();
    }

    public void setDefaultPanel(Component comp) {
        this.add(comp, "null");
        this.controlMap.put(null, comp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void activeLayerChanged(AbstractView view) {
        updateActiveView(view);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void layerAdded(int newIndex) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void layerRemoved(int oldIndex) {
    }

}
