package org.helioviewer.jhv.gui.components.base;

import java.awt.Color;

import javax.swing.border.Border;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;

@SuppressWarnings("serial")
public class JHVTableCellRenderer extends DefaultTableCellRenderer {

    protected static final Border cellBorder = new MatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY);

    @Override
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {}

    @Override
    public void invalidate() {}

    @Override
    public void validate() {}

    @Override
    public void revalidate() {}

    @Override
    public void repaint() {}

    @Override
    public void repaint(int x, int y, int width, int height) {}

    @Override
    public void setValue(Object value) {}

}
