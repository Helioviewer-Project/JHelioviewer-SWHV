package org.helioviewer.jhv.renderable.gui;

import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.renderable.components.RenderableImageLayer;

@SuppressWarnings("serial")
public class RenderableCellRenderer extends DefaultTableCellRenderer {

    @Override
    public void setValue(Object value) {
        if (value instanceof Renderable) {
            Renderable renderable = (Renderable) value;
            String layerName = renderable.getName();
            String tooltip = layerName;

            if (renderable instanceof RenderableImageLayer && ((RenderableImageLayer) renderable).isActiveImageLayer()) {
                tooltip += " (master)";
                setToolTipText(tooltip);
                setFont(UIGlobals.UIFontBold);
            } else {
                setToolTipText(null);
                setFont(UIGlobals.UIFont);
            }

            setText(layerName);
            setBorder(RenderableContainerPanel.commonBorder);
        }
    }

}
