package org.jhv.dataset.tree.models;

import java.util.Vector;
 
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
 
/**
* @author Freek Verstringe
*
*/
public class DatasetTreeModel extends DefaultTreeModel {
	private static final long serialVersionUID = -2248220527303970988L;
	private DatasetIntervals rootNode;
    
    public DatasetTreeModel(DatasetIntervals rootNode) {
    	super(rootNode);
        this.rootNode = rootNode;
        rootNode.setModel(this);

    }
    public DatasetIntervals getIntervals(){
    	return rootNode;    	
    }
 
    
}