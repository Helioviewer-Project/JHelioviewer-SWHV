package org.helioviewer.jhv.gui.components;

import java.awt.CardLayout;
import java.awt.Component;
import java.util.HashMap;

import javax.swing.JPanel;

import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.AbstractView;
import org.helioviewer.viewmodel.view.View;

@SuppressWarnings({"serial"})
public class ControlPanelContainer extends JPanel implements LayersListener {

    private HashMap<View, Component> controlMap = new HashMap<View, Component>();

    public ControlPanelContainer(Component comp) {
        this.setLayout(new CardLayout());
        this.add(comp, "null");
        this.controlMap.put(null, comp);
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

    @Override
    public void activeLayerChanged(AbstractView view) {
        updateActiveView(view);
    }

    @Override
    public void layerAdded(int newIndex) {
    }

    @Override
    public void layerRemoved(int oldIndex) {
    }

}
