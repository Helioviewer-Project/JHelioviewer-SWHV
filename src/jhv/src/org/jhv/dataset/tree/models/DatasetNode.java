package org.jhv.dataset.tree.models;

import java.util.Vector;

 
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.tree.DefaultMutableTreeNode;
 
/**
* @author Freek Verstringe
*
*/
public class DatasetNode extends DefaultMutableTreeNode {
    /**
     * The title will be displayed in the tree
     */
    private JComponent[] components;
    
    public DatasetNode(JButton[] components) {
        this.components = components;
    }  
    
    public DatasetNode(JButton button) {
        this.components = new JButton[1];
        this.components[0] = button;
    }
    
    public JComponent[] getComponents() {
		return components;
	}

	public void setcomponents(JButton[] components) {
		this.components = components;
	}
 
    private Vector<DefaultMutableTreeNode> children = new Vector<DefaultMutableTreeNode>();    
    
    public String getTitle() {
        return components[0].toString().substring(0,5);
    }
    
    /**
     * The node object should override this method to provide a text that will
     * be displayed for the node in the tree.
     */
    public String toString() {
        return components[0].toString().substring(0,25);
    }
 
}
