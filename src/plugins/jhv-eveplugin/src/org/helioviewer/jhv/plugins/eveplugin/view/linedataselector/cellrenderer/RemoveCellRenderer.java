package org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.cellrenderer;

import java.awt.Font;

import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.components.MaterialDesign;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorTablePanel;

@SuppressWarnings("serial")
public class RemoveCellRenderer extends DefaultTableCellRenderer {

    private static final String close = String.valueOf(MaterialDesign.MDI_CLOSE.getCode());
    private final Font font = UIGlobals.UIFontMDI.deriveFont(getFont().getSize2D());

    public RemoveCellRenderer() {
        setHorizontalAlignment(CENTER);
    }

    @Override
    public void setValue(Object value) {
        if (value instanceof LineDataSelectorElement && ((LineDataSelectorElement) value).isDeletable()) {
            setFont(font);
            setText(close);
        } else
            setText(null);
        setBorder(LineDataSelectorTablePanel.commonBorder);
    }

}
