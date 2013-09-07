package org.jhv.dataset.tree.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

import javax.swing.tree.TreeNode;

import org.helioviewer.jhv.layers.LayerDescriptor;
import org.jhv.dataset.tree.models.DatasetLayer;

public class DatasetType implements TreeNode{
	
	private String title;
	private DatasetInterval parent;
	
	ArrayList<DatasetLayer> datasetLayers;
	public DatasetType( String title, DatasetInterval parent){
		this.title = title;
		datasetLayers = new ArrayList<DatasetLayer>();
		this.parent = parent;
	}
    /*
     * Layers are added often at position 0 all subsequent layers are shifted by 1
     */
    public void addLayerDescriptor(final LayerDescriptor descriptor, final int idx) {
    	datasetLayers.add(idx, new DatasetLayer(descriptor, this));
    }
    
	public void addLayer(LayerDescriptor descriptor, int idx){
		datasetLayers.add(idx, new DatasetLayer(descriptor, this));
	}
	
	public void addType(LayerDescriptor descriptor){
		datasetLayers.add( new DatasetLayer(descriptor, this) );
	}
	
	public void removeType(LayerDescriptor descriptor){
		ArrayList<Integer> toRemove = new ArrayList<Integer>();
		for(int i=0; i<datasetLayers.size(); i++){
			if(datasetLayers.get(i).getDescriptor() == descriptor){
				toRemove.add(i);
			}
		}
		for(int i=toRemove.size()-1; i>=0; i--){
			datasetLayers.remove(i);
		}
	}

	public void removeLayerDescriptor(LayerDescriptor descriptor, int idx) {
		this.datasetLayers.remove(idx);
	}
	
	public boolean isEmpty() {
		if( getNumLayers() == 0){
			return true;
		}
		return false;
	}
	
	public DatasetLayer getLayer(int i) {
		return this.datasetLayers.get(i);
	}
	/*
	 * Returns the number of layers
	 */
	public int getNumLayers() {
		return this.datasetLayers.size();
	}	
	
	public String toLongString(){
		String str = "";
		str += "\t" + this.title + "\n";
		for(int i=0; i< getNumLayers() ;i++){
			str +=  this.datasetLayers.get(i).toString();
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
	public Enumeration children() {
		return Collections.enumeration(this.datasetLayers);
	}
	@Override
	public boolean getAllowsChildren() {
		return true;
	}
	@Override
	public TreeNode getChildAt(int childIndex) {
		return this.datasetLayers.get(childIndex);
	}
	@Override
	public int getChildCount() {
		return this.getNumLayers();
	}
	@Override
	public int getIndex(TreeNode node) {
		int i=0;
		while(i<this.getNumLayers() && (this.datasetLayers.get(i) != ((DatasetLayer)(node)))){
			i++;
		}
		return i;
	}
	@Override
	public TreeNode getParent() {
		return this.parent;
	}
	@Override
	public boolean isLeaf() {
		if(this.getNumLayers()>0){
			return false;
		}
		return true;
	}
};
