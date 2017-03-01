package org.helioviewer.jhv.timelines.view.linedataselector.cellrenderer;

import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.timelines.view.linedataselector.TimelinePanel;
import org.helioviewer.jhv.timelines.view.linedataselector.TimelineRenderable;

@SuppressWarnings("serial")
public class TimelineNameRenderer extends DefaultTableCellRenderer {

    @Override
    public void setValue(Object value) {
        if (value instanceof TimelineRenderable) {
            TimelineRenderable ldse = (TimelineRenderable) value;
            String layerName = ldse.getName();
            if (ldse.hasData()) {
                setText(layerName);
                setToolTipText(layerName);
            } else {
                setText("<html><font color='gray'>" + layerName);
                setToolTipText(layerName + ": No data for selected interval");
            }
        }
        setBorder(TimelinePanel.commonBorder);
    }

}
