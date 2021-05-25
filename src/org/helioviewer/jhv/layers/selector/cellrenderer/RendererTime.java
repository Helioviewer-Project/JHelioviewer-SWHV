package org.helioviewer.jhv.layers.selector.cellrenderer;

import org.helioviewer.jhv.gui.components.base.JHVTableCellRenderer;
import org.helioviewer.jhv.layers.Layer;

@SuppressWarnings("serial")
public class RendererTime extends JHVTableCellRenderer {

    @Override
    public void setValue(Object value) {
        if (value instanceof Layer) {
            setText(((Layer) value).getTimeString());
        }
    }

}
