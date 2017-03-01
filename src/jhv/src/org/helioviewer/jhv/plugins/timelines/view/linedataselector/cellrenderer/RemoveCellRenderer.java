package org.helioviewer.jhv.plugins.timelines.view.linedataselector.cellrenderer;

import java.awt.Font;

import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.gui.components.Buttons;
import org.helioviewer.jhv.plugins.timelines.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.jhv.plugins.timelines.view.linedataselector.LineDataSelectorTablePanel;

@SuppressWarnings("serial")
public class RemoveCellRenderer extends DefaultTableCellRenderer {

    private final Font font = Buttons.getMaterialFont(getFont().getSize2D());

    public RemoveCellRenderer() {
        setHorizontalAlignment(CENTER);
    }

    @Override
    public void setValue(Object value) {
        if (value instanceof LineDataSelectorElement && ((LineDataSelectorElement) value).isDeletable()) {
            setFont(font);
            setText(Buttons.close);
        } else
            setText(null);
        setBorder(LineDataSelectorTablePanel.commonBorder);
    }

}
