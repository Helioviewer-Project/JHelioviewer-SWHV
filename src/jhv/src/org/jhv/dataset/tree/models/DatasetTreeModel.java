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
    
    @Override
    public void nodesWereInserted(TreeNode parent, int[] childIndices)
    {
        Object[] children = new Object[childIndices.length];
        for (int i = 0; i < children.length; i++)
            children[i] = getChild(parent, childIndices[i]);
        TreeNode[] path = getPathToRoot(parent);
        fireTreeNodesInserted(this, getPathToRoot(parent), childIndices, children);
    }
 
    protected void fireTreeNodesInserted(Object source, Object[] path,
                                        int[] childIndices,
                                        Object[] children) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        TreeModelEvent e = null;
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==TreeModelListener.class) {
                // Lazily create the event:
                if (e == null)
                    e = new TreeModelEvent(source, path,
                                           childIndices, children);
                ((TreeModelListener)listeners[i+1]).treeStructureChanged(e);
            }
        }
    }
}