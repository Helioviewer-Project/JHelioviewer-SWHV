package org.helioviewer.jhv.renderable.gui;

import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;

@SuppressWarnings("serial")
public class RenderableVisibleCellRenderer extends DefaultTableCellRenderer {

    @Override
    public void setValue(Object value) {
        if (value instanceof Renderable) {
            Renderable renderable = (Renderable) value;
            if (renderable.isVisible()) {
                setIcon(IconBank.getIcon(JHVIcon.VISIBLE));
                setToolTipText("Click to hide");
            } else {
                setIcon(IconBank.getIcon(JHVIcon.HIDDEN));
                setToolTipText("Click to show");
            }
        }
        setText(null);
        setHorizontalAlignment(SwingConstants.CENTER);
        setBorder(RenderableContainerPanel.commonBorder);
    }

}
