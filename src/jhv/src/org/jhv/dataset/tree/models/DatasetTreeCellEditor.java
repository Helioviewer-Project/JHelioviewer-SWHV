package org.jhv.dataset.tree.models;

import java.awt.Component;
import java.util.EventObject;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;

public class DatasetTreeCellEditor extends DefaultTreeCellEditor  {
	public DatasetTreeCellEditor(JTree tree, DefaultTreeCellRenderer renderer) {
	    super(tree, renderer);
	}
	
	public Component getTreeCellEditorComponent(JTree tree, Object value,
	        boolean isSelected, boolean expanded, boolean leaf, int row) {
	    Component comp = renderer.getTreeCellRendererComponent(tree, value, true, expanded, leaf, row, true);
	    return comp;
	}
	
	public boolean isCellEditable(EventObject anEvent) {
	    return true;
	}
	
	public boolean shouldSelectCell(EventObject anEvent) {
	    return false;
	}
}
