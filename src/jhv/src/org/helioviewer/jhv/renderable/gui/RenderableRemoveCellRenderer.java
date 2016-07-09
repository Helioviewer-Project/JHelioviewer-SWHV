package org.helioviewer.jhv.renderable.gui;

import javax.swing.ImageIcon;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;

@SuppressWarnings("serial")
public class RenderableRemoveCellRenderer extends RenderableTableCellRenderer {

    private final ImageIcon icon;

    public RenderableRemoveCellRenderer(int w) {
        icon = IconBank.getIcon(JHVIcon.REMOVE_LAYER, w, w);
    }

    @Override
    public void setValue(Object value) {
        if (value instanceof Renderable) {
            if (((Renderable) value).isDeletable()) {
                setIcon(icon);
            } else {
                setIcon(null);
            }
        }
        setHorizontalAlignment(SwingConstants.CENTER);
        setBorder(RenderableContainerPanel.commonBorder);
    }

}
