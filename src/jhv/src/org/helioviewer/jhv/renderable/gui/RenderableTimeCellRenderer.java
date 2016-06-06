package org.helioviewer.jhv.renderable.gui;

import javax.swing.table.DefaultTableCellRenderer;

@SuppressWarnings("serial")
public class RenderableTimeCellRenderer extends DefaultTableCellRenderer {

    @Override
    public void setValue(Object value) {
        if (value instanceof Renderable) {
            Renderable renderable = (Renderable) value;
            setBorder(RenderableContainerPanel.commonBorder);

            String timeString = renderable.getTimeString();
            setText(timeString);

            if (timeString == null)
                setToolTipText(null);
            else
                setToolTipText("UTC time");
        }
    }

}
