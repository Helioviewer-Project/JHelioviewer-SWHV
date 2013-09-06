package org.jhv.dataset.tree.models;

import java.util.ArrayList;

import org.helioviewer.jhv.layers.LayerDescriptor;
import org.jhv.dataset.tree.models.DatasetLayer;

public class DatasetType{
	
	String title;
	
	ArrayList<DatasetLayer> datasetLayers;
	public DatasetType( String title){
		this.title = title;
		datasetLayers = new ArrayList<DatasetLayer>();
	}
    /*
     * Layers are added often at position 0 all subsequent layers are shifted by 1
     */
    public void addLayerDescriptor(final LayerDescriptor descriptor, final int idx) {
    	datasetLayers.add(idx, new DatasetLayer(descriptor));
    }
    
	public void addLayer(LayerDescriptor descriptor, int idx){
		datasetLayers.add(idx, new DatasetLayer(descriptor));
	}
	
	public void addType(LayerDescriptor descriptor){
		datasetLayers.add( new DatasetLayer(descriptor) );
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
	
	public String toString(){
		String str = "";
		str += "\t" + this.title + "\n";
		for(int i=0; i< getNumLayers() ;i++){
			str +=  this.datasetLayers.get(i).toString();
		}
		return str;
	}
};
