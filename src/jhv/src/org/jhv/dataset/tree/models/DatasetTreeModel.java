package org.jhv.dataset.tree.models;

import java.util.Vector;
 
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
 
/**
* @author Freek Verstringe
*
*/
public class DatasetTreeModel extends DefaultTreeModel {
 
   private TreeNode rootNode;
   private Vector<TreeModelListener> listeners =
       new Vector<TreeModelListener>();
    
    public DatasetTreeModel(TreeNode rootNode) {
    	super(rootNode);
        this.rootNode = rootNode;
    }
 
}