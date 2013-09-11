package org.jhv.dataset.tree.models;

import javax.swing.tree.DefaultTreeModel;
 
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