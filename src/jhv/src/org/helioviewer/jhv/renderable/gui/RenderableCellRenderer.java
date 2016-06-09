package org.helioviewer.jhv.renderable.gui;

import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.layers.RenderableImageLayer;

@SuppressWarnings("serial")
public class RenderableCellRenderer extends DefaultTableCellRenderer {

    @Override
    public void setValue(Object value) {
        if (value instanceof Renderable) {
            Renderable renderable = (Renderable) value;
            String layerName = renderable.getName();
            setText(layerName);
            if (renderable instanceof RenderableImageLayer && ((RenderableImageLayer) renderable).isActiveImageLayer()) {
                setToolTipText(layerName + " (master)");
                setFont(UIGlobals.UIFontBold);
            } else {
                setToolTipText(null);
                setFont(UIGlobals.UIFont);
            }
        }
        setBorder(RenderableContainerPanel.commonBorder);
    }

}
