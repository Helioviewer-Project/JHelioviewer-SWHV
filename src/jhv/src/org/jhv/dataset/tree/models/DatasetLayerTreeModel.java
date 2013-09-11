package org.jhv.dataset.tree.models;



import java.util.ArrayList;
import java.util.TreeMap;


import org.helioviewer.jhv.layers.LayerDescriptor;
import org.helioviewer.jhv.layers.LayersModel;


/**
* @author Freek Verstringe
*
*/
public class DatasetLayerTreeModel{
	/*
	 * Layers sorted by an interval of dates.
	 */
    private TreeMap< String, TreeMap< String, ArrayList<LayerDescriptor> > >sortedLayers;

	public DatasetLayerTreeModel() {
        super();
        sortedLayers = new TreeMap< String, TreeMap< String, ArrayList<LayerDescriptor> > >();
    }
	
	public void addInterval(String intervalKey){
		TreeMap< String, ArrayList<LayerDescriptor> > interval = new TreeMap< String, ArrayList<LayerDescriptor> >();
		sortedLayers.put(intervalKey, interval);
	}
	
	public void removeInterval(String intervalKey){
		sortedLayers.remove(intervalKey);
	}
	
	public void addType(String typeKey, String intervalKey){
		TreeMap<String, ArrayList<LayerDescriptor>> interval;
		if( !( sortedLayers.containsKey(intervalKey) ) ){
			addInterval(intervalKey);
		}
		interval = sortedLayers.get(intervalKey);
		
		if(!( sortedLayers.get(intervalKey).containsKey(typeKey) )){
			ArrayList<LayerDescriptor> type = new ArrayList<LayerDescriptor>();
			interval.put(typeKey, type);
		}
		
	}
	
	public void removeType(String typeKey, String intervalKey){
		if(sortedLayers.containsKey(intervalKey) && sortedLayers.get(intervalKey).containsKey(typeKey)){
			sortedLayers.get(intervalKey).remove(typeKey);
		}
	}
	/*
	 * Removes all types with a given key
	 */
	public void removeType(String typeKey){
    	for(TreeMap< String, ArrayList<LayerDescriptor> > types : sortedLayers.values()){
    		if(types.containsKey(types)){
    			types.remove(types);
    		}
    	}
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
    	final String interval = descriptor.getInterval();
    	final String type = descriptor.getType();
    	
    	if( !sortedLayers.containsKey(interval) || !sortedLayers.get(interval).containsKey(type)){
    		this.addType(type, interval);
    		if( descriptor!=null && type!=null ){
    			sortedLayers.get(interval).get(type).add(idx, descriptor);
    		}
    		return;
    	}
      	
    	ArrayList<LayerDescriptor> descriptors = sortedLayers.get(interval).get(type);
   		descriptors.add(idx, descriptor);
    }
    
	/*
	 * The interval and LayerTyper are inside the descriptor.
	 */
    public void removeLayer(final int idx, final String type) {
    	final LayerDescriptor descriptor = LayersModel.getSingletonInstance().getDescriptor(idx);
    	removeLayerDescriptor(descriptor, idx);
    }
    
    /*
     * When a given layer with given index is removed, all layers with a larger index
     * corresponding to the same interval and type are decremented.
     */
	public void removeLayerDescriptor(final LayerDescriptor descriptor, final int idx) {
    	final String interval = descriptor.getInterval();
    	final String type = descriptor.getType();

    	if( !sortedLayers.containsKey(interval) || !(sortedLayers.get(interval).containsKey(type) ) ){
    		return;
    	}

    	ArrayList<LayerDescriptor> descriptors = sortedLayers.get(interval).get(type);
	
		descriptors.remove(idx);
    	
    	removeEmptyTypes();
    	removeEmptyIntervals();
    }
	
	/* 
	 * Remove all empty types. This is issued after any remove.
	 *  
	 */
	public void removeEmptyTypes(){
		
    	for(TreeMap< String, ArrayList<LayerDescriptor> > intervals : sortedLayers.values()){
    		ArrayList<String> toRemove = new ArrayList<String>();
    		for(String key: intervals.keySet()){
    			if(intervals.get(key).size() == 0){
        			toRemove.add(key);
    			}
    		}
    		for(int i=0; i<toRemove.size(); i++){
    			intervals.remove(toRemove.get(i));
    		}
    	}
	}
	
	/* Remove all empty intervals */
	public void removeEmptyIntervals(){
		ArrayList<String> toRemove = new ArrayList<String>();
		for(String key: sortedLayers.keySet()){
			if(sortedLayers.get(key).size() == 0){
    			toRemove.add(key);
			}
		}
		for(int i=0; i<toRemove.size(); i++){
			sortedLayers.remove(toRemove.get(i));
		}    	
	}
	/*
	 * Getters and setters
	 */
	public TreeMap<String, TreeMap<String, ArrayList<LayerDescriptor>>> getSortedLayers() {
		return sortedLayers;
	}

	public void setSortedLayers(
			TreeMap<String, TreeMap<String, ArrayList<LayerDescriptor>>> sortedLayers) {
		this.sortedLayers = sortedLayers;
	}

	/* Get the number of current layers */
	public int getNumLayers() {
		int size = 0;
    	for(TreeMap< String, ArrayList<LayerDescriptor> > types : sortedLayers.values()){
    		for(ArrayList<LayerDescriptor> descriptors: types.values()){
    			size += descriptors.size();
    		}
    		
    	}
    	return size;
	}

	public int getNumIntervals() {
		return sortedLayers.size();
	}
	
	public int getNumTypes() {
		int size = 0;
    	for(TreeMap< String, ArrayList<LayerDescriptor> > types : sortedLayers.values()){
    			size += types.size();
    	}
		return size;
	}

	public LayerDescriptor getDescriptor(String interval, String type, int idx) {
		return sortedLayers.get(interval).get(type).get(idx);
	}
	
	public String toString(){
		String str = "";
		for(String intervalKey :sortedLayers.keySet()){
			TreeMap<String, ArrayList<LayerDescriptor>> interval;
			interval = sortedLayers.get(intervalKey);
			str += "Interval :"+ intervalKey + "\n";
			for(String typeKey: interval.keySet()){				
				ArrayList<LayerDescriptor> type = interval.get(typeKey);
				str += "\tType :"+ typeKey + "\n";
				for( int i = 0; i<type.size(); i++){
					str += "\t\t t + " + type.get(i).toString() +"\n";					
				}
			}
			
		}
		return str +"\n";
		
	}
	
	
}