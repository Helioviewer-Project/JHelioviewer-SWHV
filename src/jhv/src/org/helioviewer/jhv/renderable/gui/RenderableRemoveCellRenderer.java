package org.helioviewer.jhv.renderable.gui;

import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;

@SuppressWarnings("serial")
public class RenderableRemoveCellRenderer extends DefaultTableCellRenderer {

    @Override
    public void setValue(Object value) {
        if (value instanceof Renderable) {
            Renderable renderable = (Renderable) value;
            if (renderable.isDeletable()) {
                setIcon(IconBank.getIcon(JHVIcon.REMOVE_LAYER));
                setToolTipText("Click to remove");
            } else {
                setIcon(null); // IconBank.getIcon(JHVIcon.REMOVE_LAYER_GRAY))
                setToolTipText(null); // "Cannot be removed"
            }
        }
        setText(null);
        setHorizontalAlignment(SwingConstants.CENTER);
        setBorder(RenderableContainerPanel.commonBorder);
    }

}
