package org.helioviewer.jhv.gui.components;

import java.awt.CardLayout;
import java.awt.Component;
import java.util.HashMap;

import javax.swing.JPanel;

import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.AbstractView;

@SuppressWarnings({"serial"})
public class ControlPanelContainer extends JPanel implements LayersListener {

    private HashMap<AbstractView, Component> controlMap = new HashMap<AbstractView, Component>();

    public ControlPanelContainer(Component comp) {
        this.setLayout(new CardLayout());
        this.add(comp, "null");
        this.controlMap.put(null, comp);
        LayersModel.addLayersListener(this);
    }

    public void addLayer(AbstractView v, Component controlPanel) {
        controlMap.put(v, controlPanel);
        this.add(controlPanel, v.toString());
    }

    public void removeLayer(AbstractView v) {
        Component toRemove = controlMap.get(v);
        this.getLayout().removeLayoutComponent(toRemove);
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
        CardLayout cl = (CardLayout) this.getLayout();
        cl.show(this, view == null ? "null" : view.toString());
        ensureSize();
    }

    @Override
    public void layerAdded(AbstractView view) {
    }

}
