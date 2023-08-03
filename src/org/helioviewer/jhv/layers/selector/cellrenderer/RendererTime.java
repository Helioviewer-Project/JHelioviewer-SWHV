package org.helioviewer.jhv.layers.selector.cellrenderer;

import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.layers.Layer;

@SuppressWarnings("serial")
public final class RendererTime extends DefaultTableCellRenderer {

    @Override
    public void setValue(Object value) {
        if (value instanceof Layer layer) {
            setText(layer.getTimeString());
        }
    }

}
