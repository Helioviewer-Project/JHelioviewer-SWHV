package org.helioviewer.jhv.layers.selector.cellrenderer;

import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.components.base.JHVTableCellRenderer;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layer;

@SuppressWarnings("serial")
public class RendererName extends JHVTableCellRenderer {

    @Override
    public void setValue(Object value) {
        if (value instanceof Layer) {
            Layer layer = (Layer) value;
            String layerName = layer.getName();
            setText(layerName);
            if (layer instanceof ImageLayer && ((ImageLayer) layer).isActiveImageLayer()) {
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