package org.helioviewer.jhv.plugins.timelines.view.linedataselector.cellrenderer;

import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.plugins.timelines.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.jhv.plugins.timelines.view.linedataselector.LineDataSelectorTablePanel;

@SuppressWarnings("serial")
public class LineDataSelectorElementRenderer extends DefaultTableCellRenderer {

    @Override
    public void setValue(Object value) {
        if (value instanceof LineDataSelectorElement) {
            LineDataSelectorElement ldse = (LineDataSelectorElement) value;
            String layerName = ldse.getName();
            if (ldse.hasData()) {
                setText(layerName);
                setToolTipText(layerName);
            } else {
                setText("<html><font color='gray'>" + layerName);
                setToolTipText(layerName + ": No data for selected interval");
            }
        }
        setBorder(LineDataSelectorTablePanel.commonBorder);
    }

}
