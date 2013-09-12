package org.jhv.dataset.tree.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

import javax.swing.JPanel;
import javax.swing.tree.TreeNode;

import org.helioviewer.jhv.layers.LayerDescriptor;
import org.jhv.dataset.tree.models.DatasetLayer;
import org.jhv.dataset.tree.views.TypePanel;
import org.jhv.dataset.tree.views.LayerPanel;

public class DatasetType implements TreeNode, DatasetNode{
	
	private String title;
	private DatasetInterval parent;
	
	public ArrayList<DatasetLayer> datasetLayers;
	public DatasetType( String title, DatasetInterval parent){
		this.title = title;
		datasetLayers = new ArrayList<DatasetLayer>();
		this.parent = parent;
	}
    /*
     * Layers are added often at position 0 all subsequent layers are shifted by 1
     */
    public DatasetLayer addLayerDescriptor(final LayerDescriptor descriptor, final int idx) {
    	this.addLayer( descriptor, idx );
    	return this.getLayer(idx);
    }
    
	public DatasetTreeModel getModel(){
		return this.parent.getModel();
	}
	
	public void addLayer(LayerDescriptor descriptor, int idx){
		datasetLayers.add(idx, new DatasetLayer(descriptor, this));
		this.getModel().nodesWereInserted(this, new int[]{idx});
	}
	
	public void changeLayerDescriptor(LayerDescriptor descriptor) {
		int i=0;

		while( i<datasetLayers.size() && datasetLayers.get(i).getDescriptor() != descriptor){
			i++;
		}
		LayerPanel panel = (LayerPanel)(datasetLayers.get(i).getView());
		panel.updateChangeFast();
		this.getModel().nodeChanged(datasetLayers.get(i) );
	}	
	
	public void addLayer(LayerDescriptor descriptor){
		datasetLayers.add( new DatasetLayer(descriptor, this) );
		this.getModel().nodesWereInserted(this, new int[]{datasetLayers.size()-1});	
	}
	
	public void removeLayer(LayerDescriptor descriptor){
		ArrayList<Integer> toRemove = new ArrayList<Integer>();
		for(int i=0; i<datasetLayers.size(); i++){
			if(datasetLayers.get(i).getDescriptor() == descriptor){
				toRemove.add(i);
			}
		}
		TreeNode[] children = new TreeNode[toRemove.size()];
		int[] toRemoveints = new int[toRemove.size()];
		for(int i=0; i<toRemove.size(); i++){
			children[i] = datasetLayers.get(toRemove.get(i));
			toRemoveints[i] = toRemove.get(i);
		}				
		for(int i=toRemove.size()-1; i>=0; i--){
			datasetLayers.remove( toRemoveints[i] );
		}
		this.getModel().nodesWereRemoved(this, toRemoveints, children);
	}
	
	public void removeLayerDescriptor(LayerDescriptor descriptor) {
		this.removeLayer(descriptor);
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
	public Enumeration<DatasetLayer> children() {
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
		int i = 0;
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
    public JPanel getView() {
		return new TypePanel(this);
	}

};
