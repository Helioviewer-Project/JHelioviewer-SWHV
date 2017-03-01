package org.helioviewer.jhv.renderable.gui.cellrenderer;

import org.helioviewer.jhv.renderable.gui.Renderable;

@SuppressWarnings("serial")
public class RendererTime extends TableCellRenderer {

    @Override
    public void setValue(Object value) {
        if (value instanceof Renderable) {
            setText(((Renderable) value).getTimeString());
        }
        setBorder(TableCellRenderer.commonBorder);
    }

}
