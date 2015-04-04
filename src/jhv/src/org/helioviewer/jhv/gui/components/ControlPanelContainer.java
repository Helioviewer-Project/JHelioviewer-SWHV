package org.helioviewer.jhv.gui.components;

import java.awt.CardLayout;
import java.awt.Component;
import java.util.HashMap;

import javax.swing.JPanel;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.viewmodel.view.ImageInfoView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;

public class ControlPanelContainer extends JPanel implements LayersListener {

    private static final long serialVersionUID = 5760418851530682634L;
    HashMap<ImageInfoView, Component> controlMap = new HashMap<ImageInfoView, Component>();

    public ControlPanelContainer() {
        this.setLayout(new CardLayout());
        Displayer.getLayersModel().addLayersListener(this);
    }

    public void addLayer(ImageInfoView view, Component controlPanel) {
        controlMap.put(view, controlPanel);
        this.add(controlPanel, view.toString());
    }

    public void removeLayer(ImageInfoView view) {
        Component toRemove = controlMap.get(view);
        this.getLayout().removeLayoutComponent(toRemove);
    }

    public Component getViewComponent(ImageInfoView view) {
        return controlMap.get(view);
    }

    private void updateActiveView(View view) {
        ImageInfoView infoView = view instanceof ImageInfoView ? (ImageInfoView) view : null;

        CardLayout cl = (CardLayout) this.getLayout();
        cl.show(this, infoView == null ? "null" : infoView.toString());
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
    public void activeLayerChanged(JHVJP2View view) {
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
