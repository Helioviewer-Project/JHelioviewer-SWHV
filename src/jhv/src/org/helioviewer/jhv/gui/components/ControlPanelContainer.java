package org.helioviewer.jhv.gui.components;

import java.awt.CardLayout;
import java.awt.Component;
import java.util.HashMap;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.ImageInfoView;
import org.helioviewer.viewmodel.view.View;

public class ControlPanelContainer extends JPanel implements LayersListener {

    private static final long serialVersionUID = 5760418851530682634L;
    HashMap<ImageInfoView, Component> controlMap = new HashMap<ImageInfoView, Component>();

    public ControlPanelContainer() {
        this.setLayout(new CardLayout());
        LayersModel.getSingletonInstance().addLayersListener(this);
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

    public void updateActiveView() {
        final View activeView = LayersModel.getSingletonInstance().getActiveView();
        final ImageInfoView view = activeView != null ? activeView.getAdapter(ImageInfoView.class) : null;
        final CardLayout cl = (CardLayout) (this.getLayout());
        final ControlPanelContainer thisComp = this;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                cl.show(thisComp, view == null ? "null" : view.toString());
                thisComp.ensureSize();
            }
        });
    }

    private void ensureSize() {
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
    public void activeLayerChanged(int index) {
        this.updateActiveView();
    }

    /**
     * {@inheritDoc}
     */
    public void layerAdded(int newIndex) {
        this.updateActiveView();
    }

    /**
     * {@inheritDoc}
     */
    public void layerChanged(int index) {

        // this is called very often. refine the events to layer visibility
        // changed?
        boolean visible = LayersModel.getSingletonInstance().isVisible(index);
        View view = LayersModel.getSingletonInstance().getLayer(index);
        ImageInfoView imageInfoView = view != null ? view.getAdapter(ImageInfoView.class) : null;

        Component c = this.getViewComponent(imageInfoView);

        if (c != null) {
            c.setEnabled(visible);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void layerRemoved(View oldView, int oldIndex) {
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
