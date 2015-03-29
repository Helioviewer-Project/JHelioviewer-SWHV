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
    public void activeLayerChanged(View view) {
    }

    /**
     * NOP default implementation
     */
    public void layerAdded(int idx) {
    }

    /**
     * NOP default implementation
     */
    public void layerRemoved(int oldIdx) {
    }

}
