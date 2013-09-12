package org.jhv.dataset.tree.views;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.plaf.TreeUI;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.jhv.dataset.tree.models.DatasetNodeRenderer;
import org.jhv.dataset.tree.models.DatasetTreeCellEditor;



public class DatasetTree extends JTree{
	
	private static final long serialVersionUID = 3552416133364895287L;
	DefaultTreeModel model;
	
	public DatasetTree(DefaultTreeModel model){
		super(model);
		this.model = model;
		this.setCellRenderer( new DatasetNodeRenderer());
		this.setCellEditor(new DatasetTreeCellEditor(this, (DefaultTreeCellRenderer) this.getCellRenderer()));
		this.getModel().addTreeModelListener(new DatasetTreeModelListener(this));
		ToolTipManager.sharedInstance().registerComponent(this);
        this.addMouseMotionListener(new TreeScanner());  
        this.setRootVisible(false);
		this.setEditable(true);
	}
	
	class TreeScanner extends MouseMotionAdapter
	{
	    int lastSelected;
	 
	    public void mouseMoved(MouseEvent e)
	    {
	        JTree tree = (JTree)e.getSource();
	        int selRow = tree.getRowForLocation(e.getX(), e.getY());
	        TreePath path = getPathForLocation(e.getX(), e.getY());
	        if(selRow == -1)
	        {
	            tree.clearSelection();
	          
	            lastSelected = -1;
	        }
	        else if(selRow != lastSelected)
	        {
		        startEditingAtPath(path);
	            lastSelected = selRow;
	        }
	    }
	}
	
}
