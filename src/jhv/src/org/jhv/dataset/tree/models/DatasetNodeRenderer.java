package org.jhv.dataset.tree.models;

import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;



 
/**
* This class is implemented to customize the display of a node.
* @author Freek Verstringe
*
*/
public class DatasetNodeRenderer extends DefaultTreeCellRenderer {
    private DatasetNodePanel panel;
	private static final long serialVersionUID = -9041597414197751300L;

    
    public Component getTreeCellRendererComponent( JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row,boolean hasFocus) {
       
    	DatasetNode node = (DatasetNode) value;
    	panel = new DatasetNodePanel(node.getComponents());
    	//panel.setLayout(new BorderLayout());    	
    	//panel.add(new JLabel(node.toString()), BorderLayout.CENTER);
       
    	return panel;
    }
}
