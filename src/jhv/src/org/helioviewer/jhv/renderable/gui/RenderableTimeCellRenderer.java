package org.helioviewer.jhv.renderable.gui;

@SuppressWarnings("serial")
public class RenderableTimeCellRenderer extends RenderableTableCellRenderer {

    @Override
    public void setValue(Object value) {
        if (value instanceof Renderable) {
            setText(((Renderable) value).getTimeString());
        }
        setBorder(RenderableContainerPanel.commonBorder);
    }

}
