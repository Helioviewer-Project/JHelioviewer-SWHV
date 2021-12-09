package org.helioviewer.jhv.layers.selector.cellrenderer;

import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;

@SuppressWarnings("serial")
public class RendererName extends DefaultTableCellRenderer {

    @Override
    public void setValue(Object value) {
        if (value instanceof Layer layer) {
            String layerName = layer.getName();
            setText(layerName);
            if (layer == Layers.getActiveImageLayer()) {
                setToolTipText(layerName + " (master)");
                setFont(UIGlobals.uiFontBold);
            } else {
                setToolTipText(null);
            }
        }
    }

}
