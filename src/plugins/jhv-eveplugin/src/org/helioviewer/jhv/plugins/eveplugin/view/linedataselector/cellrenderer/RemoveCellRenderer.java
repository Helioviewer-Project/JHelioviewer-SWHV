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
        setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override
    public void setValue(Object value) {
        if (value instanceof LineDataSelectorElement) {
            setIcon(((LineDataSelectorElement) value).isDeletable() ? icon : null);
        }
        setBorder(LineDataSelectorTablePanel.commonBorder);
    }

}
