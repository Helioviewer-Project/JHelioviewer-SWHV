package org.helioviewer.jhv.timelines.view.linedataselector.cellrenderer;

import java.awt.Color;

import org.helioviewer.jhv.gui.components.base.JHVTableCellRenderer;
import org.helioviewer.jhv.timelines.view.linedataselector.TimelineRenderable;

@SuppressWarnings("serial")
public class TimelineNameRenderer extends JHVTableCellRenderer {

    @Override
    public void setValue(Object value) {
        if (value instanceof TimelineRenderable) {
            TimelineRenderable ldse = (TimelineRenderable) value;
            String layerName = ldse.getName();
            if (ldse.hasData()) {
                setToolTipText(layerName);
            } else {
                setForeground(Color.GRAY);
                setToolTipText(layerName + ": No data for selected interval");
            }
            setText(layerName);
        }
        setBorder(cellBorder);
    }

}
