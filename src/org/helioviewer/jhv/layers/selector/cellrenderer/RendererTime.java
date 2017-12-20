package org.helioviewer.jhv.layers.selector.cellrenderer;

import org.helioviewer.jhv.gui.components.base.JHVTableCellRenderer;
import org.helioviewer.jhv.layers.selector.Renderable;

@SuppressWarnings("serial")
public class RendererTime extends JHVTableCellRenderer {

    @Override
    public void setValue(Object value) {
        if (value instanceof Renderable) {
            setText(((Renderable) value).getTimeString());
        }
        setBorder(cellBorder);
    }

}
