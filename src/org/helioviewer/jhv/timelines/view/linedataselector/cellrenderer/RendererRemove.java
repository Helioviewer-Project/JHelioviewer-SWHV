package org.helioviewer.jhv.timelines.view.linedataselector.cellrenderer;

import java.awt.Font;

import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.gui.components.base.JHVTableCellRenderer;
import org.helioviewer.jhv.timelines.view.linedataselector.TimelineRenderable;

@SuppressWarnings("serial")
public class RendererRemove extends JHVTableCellRenderer {

    private final Font font = Buttons.getMaterialFont(getFont().getSize2D());

    public RendererRemove() {
        setHorizontalAlignment(CENTER);
    }

    @Override
    public void setValue(Object value) {
        if (value instanceof TimelineRenderable && ((TimelineRenderable) value).isDeletable()) {
            setFont(font);
            setText(Buttons.close);
        } else
            setText(null);
        setBorder(cellBorder);
    }

}
