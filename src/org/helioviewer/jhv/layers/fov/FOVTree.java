package org.helioviewer.jhv.layers.fov;

import java.awt.Component;
import java.util.EventObject;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;

@SuppressWarnings("serial")
class FOVTree extends JTree {

    FOVTree(TreeModel model) {
        FOVTreeRenderer ftr = new FOVTreeRenderer();
        setModel(model);
        setEditable(true);
        setShowsRootHandles(true);
        setCellRenderer(ftr);
        setCellEditor(new MyTreeCellEditor(this, ftr));
        setRowHeight(0); // force calculation of nodes heights
    }

    private static class MyTreeCellEditor extends DefaultTreeCellEditor {

        MyTreeCellEditor(JTree tree, DefaultTreeCellRenderer renderer) {
            super(tree, renderer);
        }

        @Override
        public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row) {
            return renderer.getTreeCellRendererComponent(tree, value, true, expanded, leaf, row, true);
        }

        @Override
        public boolean isCellEditable(EventObject e) {
            return true;
        }

    }

}
