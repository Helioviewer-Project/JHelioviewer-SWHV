package org.helioviewer.jhv.renderable.gui;

import javax.swing.SwingConstants;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;

@SuppressWarnings("serial")
public class RenderableRemoveCellRenderer extends RenderableTableCellRenderer {

    @Override
    public void setValue(Object value) {
        if (value instanceof Renderable) {
            Renderable renderable = (Renderable) value;
            if (renderable.isDeletable()) {
                setIcon(IconBank.getIcon(JHVIcon.REMOVE_LAYER));
            } else {
                setIcon(null); // IconBank.getIcon(JHVIcon.REMOVE_LAYER_GRAY))
            }
        }
        setHorizontalAlignment(SwingConstants.CENTER);
        setBorder(RenderableContainerPanel.commonBorder);
    }

}
