package org.helioviewer.jhv.layers.selector.cellrenderer;

import java.awt.Font;

import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.gui.components.base.JHVTableCellRenderer;
import org.helioviewer.jhv.layers.Layer;

@SuppressWarnings("serial")
public class RendererRemove extends JHVTableCellRenderer {

    private final Font font = Buttons.getMaterialFont(UIGlobals.uiFont.getSize2D());

    public RendererRemove() {
        setHorizontalAlignment(CENTER);
    }

    @Override
    public void setValue(Object value) {
        if (value instanceof Layer && ((Layer) value).isDeletable()) {
            setFont(font);
            setText(Buttons.close);
        } else
            setText(null);
        setBorder(cellBorder);
    }

}
