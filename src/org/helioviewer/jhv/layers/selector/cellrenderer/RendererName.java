package org.helioviewer.jhv.layers.selector.cellrenderer;

import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.components.base.JHVTableCellRenderer;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.selector.Renderable;

@SuppressWarnings("serial")
public class RendererName extends JHVTableCellRenderer {

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
        setBorder(cellBorder);
    }

}
