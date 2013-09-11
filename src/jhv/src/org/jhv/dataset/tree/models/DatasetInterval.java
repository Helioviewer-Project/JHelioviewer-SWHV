package org.jhv.dataset.tree.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

import javax.swing.JPanel;
import javax.swing.tree.TreeNode;

import org.helioviewer.jhv.layers.LayerDescriptor;
import org.jhv.dataset.tree.models.DatasetType;
import org.jhv.dataset.tree.views.IntervalPanel;

public class DatasetInterval implements TreeNode, DatasetNode{
	private String title;
	ArrayList<DatasetType> datasetTypes;
	DatasetIntervals parent;
	boolean removeEmptyTypes;
	
	public DatasetInterval( String title, DatasetIntervals parent ){
		this.removeEmptyTypes = true;
		this.parent = parent;
		this.title = title;
		datasetTypes = new ArrayList<DatasetType>();
	}
	
	public DatasetTreeModel getModel(){
		return this.parent.getModel();
	}
	
    public DatasetType getType(String title){
    	DatasetType datasetType = null;
    	int i=0;
    	while( datasetType==null && i<datasetTypes.size()){
    		if(datasetTypes.get(i).getTitle().equals(title)){
    			datasetType = this.getType(i);
    		}
    		i++;
    	}
    	return datasetType;
    }
    
    public int getTypeIndex(String title){
    	DatasetType datasetType = null;
    	int i=0;
    	while( datasetType==null && i<datasetTypes.size()){
    		if( datasetTypes.get(i).getTitle().equals(title) ){
    			datasetType = this.getType(i);
    		}
    		i++;
    	}
    	return i;
    }
    
	public DatasetType getType(int idx) {
		return datasetTypes.get(idx);
	}    
    /*
     * Layers are added often at position 0 all subsequent layers are shifted by 1
     */
    public DatasetLayer addLayerDescriptor(final LayerDescriptor descriptor, final int idx) {
    	final String typeTitle = descriptor.getType();
    	
    	DatasetType datasetType = getType(typeTitle);
		if( datasetType == null ){
			this.addType(typeTitle);
			datasetType = getType(typeTitle);
		}
		return datasetType.addLayerDescriptor(descriptor, idx);
    }
    
	public void addType(String title, int idx){
		datasetTypes.add(idx, new DatasetType(title, this));
		this.getModel().nodesWereInserted(this, new int[]{idx});
	}
	
	public void addType(String title){
		datasetTypes.add( new DatasetType(title, this) );
		this.getModel().nodesWereInserted(this, new int[]{datasetTypes.size()-1});
	}
	
	public void removeType(int[] toRemoveints){
		TreeNode[] children = new TreeNode[toRemoveints.length];
		for(int i=0; i<toRemoveints.length; i++){
			children[i] = datasetTypes.get(toRemoveints[i]);
		}				
		for(int i=toRemoveints.length-1; i>=0; i--){
			datasetTypes.remove(i);
		}
		this.getModel().nodesWereRemoved(this, toRemoveints, children);
	}	
	
	public void removeType(String title){
		ArrayList<Integer> toRemove = new ArrayList<Integer>();
		for(int i=0; i<datasetTypes.size(); i++){
			if(datasetTypes.get(i).getTitle() == title){
				toRemove.add(i);
			}
		}
		
		int[] toRemoveints = new int[toRemove.size()];
		for(int i=0; i<toRemove.size(); i++){
			toRemoveints[i] = toRemove.get(i);
		}
		
		removeType(toRemoveints);
	}

	public void removeLayerDescriptor(LayerDescriptor descriptor, int idx) {
    	final String typeTitle = descriptor.getType();
    	DatasetType datasetType = getType(typeTitle);
    	datasetType.removeLayerDescriptor(descriptor , idx);
    	if(this.removeEmptyTypes){
    		removeEmptyTypes();
		}
    }

	public boolean isEmpty() {
		if(this.datasetTypes.size()==0){
			return true;
		}
		return false;
	}

	/*
	 * Removes empty types
	 */
    public void removeEmptyTypes() {
    	for( int i = this.getNumTypes()-1; i>=0; i-- ){
    		if(this.datasetTypes.get(i).isEmpty()){
    			this.removeType( new int[]{i} );
    		}
    	}
    }

	public int getNumTypes() {
		return this.datasetTypes.size();
	}

	public int getNumLayers() {
		int count = 0;
		for(int i =0; i<this.datasetTypes.size(); i++){
			count += this.datasetTypes.get(i).getNumLayers();
		}
		return count;
	}
	
	public String toLongString(){
		String str = "";
		str += this.title + "\n";
		for(int i=0; i< getNumTypes() ;i++){
			str += this.datasetTypes.get(i).toLongString();
		}
		return str;
	}
	public String toString() {
		return this.title;
	}
	public String getTitle() {
		return this.title;
	}

	@Override
	public Enumeration<DatasetType> children() {
		return Collections.enumeration(this.datasetTypes);
	}

	@Override
	public boolean getAllowsChildren() {
		return true;
	}

	@Override
	public TreeNode getChildAt(int childIndex) {
		return this.datasetTypes.get(childIndex);
	}

	@Override
	public int getChildCount() {
		return this.getNumTypes();
	}

	@Override
	public int getIndex(TreeNode node) {
		return this.getTypeIndex( ((DatasetType)(node)).getTitle() );
	}

	@Override
	public TreeNode getParent() {
		return (TreeNode)(this.parent);
	}

	@Override
	public boolean isLeaf() {
		return false;
	}
	
    public JPanel getView() {
		return new IntervalPanel(this);
	}
};
