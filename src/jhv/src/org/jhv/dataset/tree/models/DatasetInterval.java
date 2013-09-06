package org.jhv.dataset.tree.models;

import java.util.ArrayList;

import org.helioviewer.jhv.layers.LayerDescriptor;
import org.jhv.dataset.tree.models.DatasetType;

public class DatasetInterval{
	private String title;
	ArrayList<DatasetType> datasetTypes;

	public DatasetInterval( String title ){
		this.title = title;
		datasetTypes = new ArrayList<DatasetType>();
	}
	
    public DatasetType getType(String title){
    	DatasetType datasetType = null;
    	int i=0;
    	while( datasetType==null && i<datasetTypes.size()){
    		if(datasetTypes.get(i).title == title){
    			datasetType = datasetTypes.get(i);
    		}
    		i++;
    	}
    	return datasetType;
    }
    /*
     * Layers are added often at position 0 all subsequent layers are shifted by 1
     */
    public void addLayerDescriptor(final LayerDescriptor descriptor, final int idx) {
    	final String typeTitle = descriptor.getType();
    	
    	DatasetType datasetType = getType(typeTitle);
		if( datasetType==null ){
			datasetType = new DatasetType(typeTitle);
			datasetTypes.add(datasetType);
		}
		datasetType.addLayerDescriptor(descriptor, idx);
    }
    
	public void addType(String title, int idx){
		datasetTypes.add(idx, new DatasetType(title));
	}
	public void addType(String title){
		datasetTypes.add( new DatasetType(title) );
	}
	public void removeType(String title){
		ArrayList<Integer> toRemove = new ArrayList<Integer>();
		for(int i=0; i<datasetTypes.size(); i++){
			if(datasetTypes.get(i).title == title){
				toRemove.add(i);
			}
		}
		for(int i=toRemove.size()-1; i>=0; i--){
			datasetTypes.remove(i);
		}
	}

	public void removeLayerDescriptor(LayerDescriptor descriptor, int idx) {
    	final String typeTitle = descriptor.getType();
    	DatasetType datasetType = getType(typeTitle);
    	datasetType.removeLayerDescriptor(descriptor , idx);
    }

	public boolean isEmpty() {
		if(this.datasetTypes.size()==0){
			return true;
		}
		return false;
	}

	/*
	 * Removes empty intervals
	 */
    public void removeEmptyTypes() {
    	for( int i = this.getNumTypes()-1; i>=0; i++ ){
    		if(this.datasetTypes.get(i).isEmpty()){
    			this.datasetTypes.remove(i);
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
	
	public String toString(){
		String str = "";
		str += this.title + "\n";
		for(int i=0; i< getNumTypes() ;i++){
			str += this.datasetTypes.get(i).toString();
		}
		return str;
	}

	public String getTitle() {
		return this.title;
	}
};
