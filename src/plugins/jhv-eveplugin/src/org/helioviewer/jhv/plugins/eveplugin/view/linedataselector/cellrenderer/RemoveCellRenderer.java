package org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.cellrenderer;

import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorTablePanel;

@SuppressWarnings("serial")
public class RemoveCellRenderer extends DefaultTableCellRenderer {

    @Override
    public void setValue(Object value) {
        if (value instanceof LineDataSelectorElement) {
            LineDataSelectorElement lineDataElement = (LineDataSelectorElement) value;
            if (lineDataElement.isDeletable()) {
                setIcon(IconBank.getIcon(JHVIcon.REMOVE_LAYER));
                setToolTipText("Click to remove");
            } else {
                setIcon(null);
                setToolTipText(null);
            }
        }
        setText(null);
        setHorizontalAlignment(SwingConstants.CENTER);
        setBorder(LineDataSelectorTablePanel.commonBorder);
    }

}
