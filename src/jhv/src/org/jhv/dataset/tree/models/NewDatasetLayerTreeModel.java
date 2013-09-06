package org.jhv.dataset.tree.models;

import java.util.ArrayList;
import java.util.TreeMap;

import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.layers.LayerDescriptor;
import org.helioviewer.jhv.layers.LayersModel;


/**
* @author Freek Verstringe
*
*/
public class NewDatasetLayerTreeModel{
	
	/*
	 * Layers sorted by an interval of dates.
	 */
    private ArrayList<DatasetInterval> datasetIntervals;
    private boolean removeEmptyIntervals;
    /*
     * Constructors
     */
	public NewDatasetLayerTreeModel() {
        super();
        this.removeEmptyIntervals = true;
        datasetIntervals = new ArrayList<DatasetInterval>();
    }
	
	public NewDatasetLayerTreeModel(boolean removeEmptyIntervals) {
        super();
        this.removeEmptyIntervals = removeEmptyIntervals;
        datasetIntervals = new ArrayList<DatasetInterval>();
    }
	/*
	 * Interval lookup by String value. Returns null if not found.
	 */
    public DatasetInterval getInterval(String title){
    	DatasetInterval datasetInterval = null;
    	int i=0;
    	while( datasetInterval==null && i<datasetIntervals.size()){
    		if(datasetIntervals.get(i).getTitle() == title){
    			datasetInterval = datasetIntervals.get(i);
    		}
    		i++;
    	}
    	return datasetInterval;
    }
    
	/*
	 * Interval index lookup by String value. -1 if not found.
	 */
    public int getIntervalIndex(String title){
    	DatasetInterval datasetInterval = null;
    	int i=0;
    	while( datasetInterval==null && i<datasetIntervals.size()){
    		if(datasetIntervals.get(i).getTitle() == title){
    			datasetInterval = datasetIntervals.get(i);
    		}
    		i++;
    	}
    	
    	if(i == datasetIntervals.size()){
    		return -1;
    	}
    	
    	return i;
    }	
    
	/*
	 * The interval and LayerTyper are inside the descriptor.
	 */
    public void addLayer(final int idx, final String type) {
    	final LayerDescriptor descriptor = LayersModel.getSingletonInstance().getDescriptor(idx);
    	addLayerDescriptor(descriptor, idx);
    }	

    /*
     * Layers are added often at position 0 all subsequent layers are shifted by 1
     */
    public void addLayerDescriptor(final LayerDescriptor descriptor, final int idx) {
    	final String intervalTitle = descriptor.getInterval();
    	
    	DatasetInterval datasetInterval = getInterval(intervalTitle);
		if( datasetInterval==null ){
			datasetInterval = new DatasetInterval(intervalTitle);
			datasetIntervals.add(datasetInterval);
		}
		datasetInterval.addLayerDescriptor(descriptor, idx);
    }	
	
	public void addInterval(String title){
		this.addInterval( title, 0 );
	}
	
	public void addInterval(String title, int idx){
		datasetIntervals.add(idx, new DatasetInterval(title));
	}
	
	public void addType(String title){
		datasetIntervals.add( new DatasetInterval(title) );
	}
	
	public void removeInterval( String title ){
		int index = getIntervalIndex(title);
		if( index!=-1 ){			
			removeInterval(index);
		}
	}	
	
	public void removeInterval( int index ){
		datasetIntervals.remove(index);
	}
	
	/*
	 * The interval and LayerTyper are inside the descriptor.
	 */
    public void removeLayerDescriptor(final LayerDescriptor descriptor, final int idx) {
    	final String intervalTitle = descriptor.getInterval();
    	DatasetInterval datasetInterval = getInterval(intervalTitle);
    	datasetInterval.removeLayerDescriptor(descriptor , idx);
    }
    
	/*
	 * Removes empty intervals
	 */
    public void removeEmptyIntervals() {
    	for( int i = this.getNumIntervals()-1; i>=0; i++ ){
    		if(this.datasetIntervals.get(i).isEmpty()){
    			this.datasetIntervals.remove(i);
    		}
    	}
    }
    
	/*
	 * Removes empty types recursively
	 */
    public void removeEmptyTypes() {
    	for( int i = this.getNumIntervals()-1; i>=0; i++ ){
    			this.datasetIntervals.get(i).removeEmptyTypes();    		
    	}
    }
    
	public int getNumIntervals() {
		return this.datasetIntervals.size();
	}

	public LayerDescriptor getDescriptor(String interval, String type, int i) {
		
		DatasetLayer datasetLayer = this.getInterval(interval).getType(type).getLayer(i);
		return datasetLayer.getDescriptor();
	}

	public int getNumLayers() {
		int count = 0;
		for(int i=0; i<this.datasetIntervals.size(); i++){
			count += datasetIntervals.get(i).getNumLayers();
		}
		return count;
	}

	public int getNumTypes() {
		int count = 0;
		for(int i=0; i<this.datasetIntervals.size(); i++){
			count += datasetIntervals.get(i).getNumTypes();
		}
		return count;
	}

	public ArrayList<DatasetInterval> getIntervals() {
		
		return this.datasetIntervals;
	}

	public void addType(String typeKey, String intervalKey) {
		if(this.getInterval(intervalKey) ==null){
			this.addInterval(intervalKey);
		}
		DatasetInterval datasetInterval = this.getInterval(intervalKey);
		if( datasetInterval.getType(typeKey) == null ){
			datasetInterval.addType(typeKey);
		}
	}   
	public String toString(){
		String str = "";
		for(int i=0; i< getNumIntervals() ;i++){
			str += this.datasetIntervals.get(i).toString();
		}
		return str;
	}
		
}