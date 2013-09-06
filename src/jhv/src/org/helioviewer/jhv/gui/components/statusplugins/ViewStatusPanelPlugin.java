package org.helioviewer.jhv.gui.components.statusplugins;

import javax.swing.JLabel;

import org.helioviewer.jhv.gui.interfaces.StatusPanelPlugin;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.viewmodel.view.View;

@SuppressWarnings("serial")
public abstract class ViewStatusPanelPlugin extends JLabel implements StatusPanelPlugin, LayersListener {

    /**
     * NOP default implementation
     */
    public void activeLayerChanged(int idx) {
    }

    /**
     * NOP default implementation
     */
    public void layerAdded(int idx) {
    }

    /**
     * NOP default implementation
     */
    public void layerChanged(int idx) {
    }

    /**
     * NOP default implementation
     */
    public void layerRemoved(View oldView, int oldIdx) {
    }

    /**
     * NOP default implementation
     */
    public void viewportGeometryChanged() {
    }

    /**
     * NOP default implementation
     */
    public void timestampChanged(int idx) {
    }

    /**
     * NOP default implementation
     */
    public void subImageDataChanged() {
    }

    /**
     * NOP default implementation
     */
    public void layerDownloaded(int idx) {
    }

}
