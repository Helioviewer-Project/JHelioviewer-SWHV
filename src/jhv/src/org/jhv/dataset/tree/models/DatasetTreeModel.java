package org.jhv.dataset.tree.models;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;

import org.helioviewer.jhv.layers.LayerDescriptor;
 
/**
* @author Freek Verstringe
*
*/
public class DatasetTreeModel extends DefaultTreeModel {
	private static final long serialVersionUID = -2248220527303970988L;
	private DatasetIntervals intervals;
	private JTree tree;
    
    public DatasetTreeModel(DatasetIntervals intervals) {
    	super(intervals);
        this.intervals = intervals;
        intervals.setModel(this);

    }
    
    public DatasetIntervals getIntervals(){
    	return intervals;    	
    }
    
	public void setTree(JTree tree) {
		this.tree = tree;		
	}
	
	public JTree getTree() {
		return( this.tree );		
	}
	
	public void changeLayerDescriptor(LayerDescriptor descriptor) {
		this.intervals.changeLayerDescriptor(descriptor);		
	}
	
	public void removeLayerDescriptor(LayerDescriptor descriptor) {
		this.intervals.removeLayerDescriptor(descriptor);	
	}
	
	public DatasetLayer addLayerDescriptor(LayerDescriptor descriptor, int index) {
		return this.intervals.addLayerDescriptor(descriptor, index);	
	}
    
}