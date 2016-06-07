package org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.cellrenderer;

import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorTablePanel;

@SuppressWarnings("serial")
public class LineDataVisibleCellRenderer extends DefaultTableCellRenderer {

    @Override
    public void setValue(Object value) {
        if (value instanceof LineDataSelectorElement) {
            LineDataSelectorElement lineDataElement = (LineDataSelectorElement) value;
            if (lineDataElement.isVisible()) {
                setIcon(IconBank.getIcon(JHVIcon.VISIBLE));
                setToolTipText("Click to hide");
            } else {
                setIcon(IconBank.getIcon(JHVIcon.HIDDEN));
                setToolTipText("Click to show");
            }
        }
        setText(null);
        setHorizontalAlignment(SwingConstants.CENTER);
        setBorder(LineDataSelectorTablePanel.commonBorder);
    }

}
