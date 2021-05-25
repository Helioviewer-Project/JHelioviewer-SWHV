package org.helioviewer.jhv.timelines.selector.cellrenderer;

import java.awt.Font;

import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.gui.components.base.JHVTableCellRenderer;
import org.helioviewer.jhv.timelines.TimelineLayer;

@SuppressWarnings("serial")
public class RendererRemove extends JHVTableCellRenderer {

    private final Font font = Buttons.getMaterialFont(getFont().getSize2D());

    @Override
    public void setValue(Object value) {
        setBorder(null); //!
        if (value instanceof TimelineLayer && ((TimelineLayer) value).isDeletable()) {
            setFont(font);
            setText(Buttons.close);
        } else
            setText(null);
    }

}
