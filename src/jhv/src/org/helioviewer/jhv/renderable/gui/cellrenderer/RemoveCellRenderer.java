package org.helioviewer.jhv.renderable.gui.cellrenderer;

import java.awt.Font;

import javax.swing.SwingConstants;

import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.components.MaterialDesign;
import org.helioviewer.jhv.renderable.gui.Renderable;

@SuppressWarnings("serial")
public class RemoveCellRenderer extends TableCellRenderer {

    private static final String close = String.valueOf(MaterialDesign.MDI_CLOSE.getCode());
    private final Font font = UIGlobals.UIFontMDI.deriveFont(getFont().getSize2D());

    public RemoveCellRenderer() {
        setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override
    public void setValue(Object value) {
        if (value instanceof Renderable && ((Renderable) value).isDeletable()) {
            setFont(font);
            setText(close);
        } else
            setText(null);
        setBorder(TableCellRenderer.commonBorder);
    }

}
