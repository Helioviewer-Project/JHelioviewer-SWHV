package org.helioviewer.jhv.renderable.gui;

import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.layers.ImageLayer;

@SuppressWarnings("serial")
public class RenderableCellRenderer extends RenderableTableCellRenderer {

    @Override
    public void setValue(Object value) {
        if (value instanceof Renderable) {
            Renderable renderable = (Renderable) value;
            String layerName = renderable.getName();
            setText(layerName);
            if (renderable instanceof ImageLayer && ((ImageLayer) renderable).isActiveImageLayer()) {
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
