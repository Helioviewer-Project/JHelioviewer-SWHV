package org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.cellrenderer;

import javax.swing.ImageIcon;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorTablePanel;

@SuppressWarnings("serial")
public class RemoveCellRenderer extends DefaultTableCellRenderer {

    private final ImageIcon icon;

    public RemoveCellRenderer(int w) {
        icon = IconBank.getIcon(JHVIcon.REMOVE_LAYER, w, w);
    }

    @Override
    public void setValue(Object value) {
        if (value instanceof LineDataSelectorElement) {
            if (((LineDataSelectorElement) value).isDeletable()) {
                setIcon(icon);
            } else {
                setIcon(null);
            }
        }
        setHorizontalAlignment(SwingConstants.CENTER);
        setBorder(LineDataSelectorTablePanel.commonBorder);
    }

}
