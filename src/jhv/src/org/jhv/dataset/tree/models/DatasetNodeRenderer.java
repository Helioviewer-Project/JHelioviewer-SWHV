package org.jhv.dataset.tree.models;

import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 * This class is implemented to customize the display of a node.
 * 
 * @author Freek Verstringe
 * 
 */
public class DatasetNodeRenderer extends DefaultTreeCellRenderer {
    private static final long serialVersionUID = -9041597414197751300L;

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        this.setToolTipText("sdf");
        DatasetNode node = (DatasetNode) value;
        return node.getView();
    }
}
