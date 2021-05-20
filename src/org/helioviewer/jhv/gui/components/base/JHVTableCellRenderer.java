package org.helioviewer.jhv.gui.components.base;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.base.Colors;

@SuppressWarnings("serial")
public class JHVTableCellRenderer extends DefaultTableCellRenderer {

    public static final Border cellBorder = BorderFactory.createMatteBorder(0, 0, 1, 0, Colors.lightGray);
    public static final Border cellEmphasisBorder = BorderFactory.createMatteBorder(0, 0, 1, 0, Colors.darkGray);

    @Override
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
    }

    @Override
    public void invalidate() {
    }

    @Override
    public void validate() {
    }

    @Override
    public void revalidate() {
    }

    @Override
    public void repaint() {
    }

    @Override
    public void repaint(int x, int y, int width, int height) {
    }

    @Override
    public void setValue(Object value) {
    }

}
