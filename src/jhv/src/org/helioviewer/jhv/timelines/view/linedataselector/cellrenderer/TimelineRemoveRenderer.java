package org.helioviewer.jhv.timelines.view.linedataselector.cellrenderer;

import java.awt.Font;

import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.timelines.view.linedataselector.TimelinePanel;
import org.helioviewer.jhv.timelines.view.linedataselector.TimelineRenderable;

@SuppressWarnings("serial")
public class TimelineRemoveRenderer extends DefaultTableCellRenderer {

    private final Font font = Buttons.getMaterialFont(getFont().getSize2D());

    public TimelineRemoveRenderer() {
        setHorizontalAlignment(CENTER);
    }

    @Override
    public void setValue(Object value) {
        if (value instanceof TimelineRenderable && ((TimelineRenderable) value).isDeletable()) {
            setFont(font);
            setText(Buttons.close);
        } else
            setText(null);
        setBorder(TimelinePanel.commonBorder);
    }

}
