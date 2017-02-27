package org.helioviewer.jhv.renderable.gui.cellrenderer;

import java.awt.Font;

import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.renderable.gui.Renderable;

@SuppressWarnings("serial")
public class RemoveCellRenderer extends TableCellRenderer {

    private final Font font = Buttons.getMaterialFont(getFont().getSize2D());

    public RemoveCellRenderer() {
        setHorizontalAlignment(CENTER);
    }

    @Override
    public void setValue(Object value) {
        if (value instanceof Renderable && ((Renderable) value).isDeletable()) {
            setFont(font);
            setText(Buttons.close);
        } else
            setText(null);
        setBorder(TableCellRenderer.commonBorder);
    }

}
