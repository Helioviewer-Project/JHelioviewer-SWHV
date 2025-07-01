package org.helioviewer.jhv.gui.components.base;

import java.awt.Component;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.helioviewer.jhv.gui.Interfaces;

public class JHVTreeCell {

    @SuppressWarnings("serial")
    public static class Renderer extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            if (value instanceof Interfaces.JHVCell cell) {
                return cell.getComponent();
            } else
                return super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        }

    }

    @SuppressWarnings("serial")
    public static class Editor extends DefaultCellEditor {

        public Editor() {
            super(new JCheckBox());
        }

        @Override
        public Component getTreeCellEditorComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row) {
            if (value instanceof Interfaces.JHVCell cell) {
                return cell.getComponent();
            } else
                return super.getTreeCellEditorComponent(tree, value, selected, expanded, leaf, row);
        }

    }

}
