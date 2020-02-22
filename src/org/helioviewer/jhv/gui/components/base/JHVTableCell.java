package org.helioviewer.jhv.gui.components.base;

import java.awt.Component;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.gui.interfaces.JHVCell;

public class JHVTableCell {

    @SuppressWarnings("serial")
    public static class Renderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof JHVCell) {
                return ((JHVCell) value).getComponent();
            } else
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }

    }

    @SuppressWarnings("serial")
    public static class Editor extends DefaultCellEditor {

        public Editor() {
            super(new JCheckBox());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (value instanceof JHVCell) {
                return ((JHVCell) value).getComponent();
            } else
                return super.getTableCellEditorComponent(table, value, isSelected, row, column);
        }

    }

}
