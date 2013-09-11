package org.jhv.dataset.tree.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;


import javax.swing.JPanel;
import javax.swing.tree.TreeNode;

import org.helioviewer.jhv.layers.LayerDescriptor;
import org.helioviewer.jhv.layers.LayersModel;
import org.jhv.dataset.tree.views.IntervalsPanel;


/**
* @author Freek Verstringe
*
*/
public class DatasetIntervals implements TreeNode, DatasetNode{
	
	/*
	 * Layers sorted by an interval of dates.
	 */
    private ArrayList<DatasetInterval> datasetIntervals;
    private boolean removeEmptyIntervals;
    private DatasetTreeModel model;
    /*
     * Constructors
     */
	public DatasetIntervals() {
        super();
        this.removeEmptyIntervals = true;
        datasetIntervals = new ArrayList<DatasetInterval>();
    }
	
	public DatasetTreeModel getModel(){
		return this.model;
	}
	public void setModel(DatasetTreeModel model){
        this.model = model;
	}
	
	public DatasetIntervals(boolean removeEmptyIntervals) {
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
    		String ht = datasetIntervals.get(i).getTitle();
    		if( ht.equals(title) ){
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
    		if(datasetIntervals.get(i).getTitle().equals(title) ){
    			datasetInterval = getInterval(i);
    		}
    		i++;
    	}
    	
    	if(i == datasetIntervals.size()){
    		return -1;
    	}
    	
    	return i;
    }	

	public DatasetInterval getInterval(int idx) {
		// TODO Auto-generated method stub
		return datasetIntervals.get(idx);
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
    public DatasetLayer addLayerDescriptor(final LayerDescriptor descriptor, final int idx) {
    	final String intervalTitle = descriptor.getInterval();
    	
    	DatasetInterval datasetInterval = getInterval(intervalTitle);
		if( datasetInterval==null ){
			this.addInterval(intervalTitle);
			datasetInterval = getInterval(intervalTitle);
		}
		return datasetInterval.addLayerDescriptor(descriptor, idx);
    }	
	
	public void addInterval(String title){
		this.addInterval( title, 0 );
	}
	
	public void addInterval(String title, int idx){
		datasetIntervals.add(idx, new DatasetInterval(title, this));
		this.getModel().nodesWereInserted(this, new int[]{idx});
	}
	
	
	public void removeInterval( String title ){
		int index = getIntervalIndex(title);
		if( index!=-1 ){			
			removeInterval(index);
		}
	}	
	
	public void removeInterval( int index ){
		TreeNode[] toRemove = new TreeNode[]{datasetIntervals.get(index)};
		int[]indices_to_remove = new int[]{index};
		datasetIntervals.remove(index);	
		this.getModel().nodesWereRemoved(this, indices_to_remove, toRemove);		
	}
	
	/*
	 * The interval and LayerTyper are inside the descriptor.
	 */
    public void removeLayerDescriptor(final LayerDescriptor descriptor, final int idx) {
    	final String intervalTitle = descriptor.getInterval();
    	DatasetInterval datasetInterval = getInterval(intervalTitle);
    	datasetInterval.removeLayerDescriptor(descriptor , idx);
    	if(this.removeEmptyIntervals){
    		removeEmptyIntervals();
    	}
    }
    
	/*
	 * Removes empty intervals
	 */
    public void removeEmptyIntervals() {
    	for( int i = this.getNumIntervals()-1; i>=0; i-- ){
    		if(this.datasetIntervals.get(i).isEmpty()){
    			this.removeInterval(i);
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
	public String toLongString(){
		String str = "";
		for(int i=0; i< getNumIntervals() ;i++){
			str += this.datasetIntervals.get(i).toLongString();
		}
		return str;
	}
	public String toString() {
		return "main";
	}
	
	@Override
	public Enumeration<DatasetInterval> children() {
		return Collections.enumeration(this.datasetIntervals);
	}

	@Override
	public boolean getAllowsChildren() {
		return true;
	}

	@Override
	public TreeNode getChildAt(int childIndex) {
		return this.datasetIntervals.get(childIndex);
	}

	@Override
	public int getChildCount() {
		return this.getNumIntervals();
	}

	@Override
	public int getIndex(TreeNode node) {
		return this.getIntervalIndex(((DatasetInterval)node).getTitle());
	}

	@Override
	public TreeNode getParent() {
		return null;
	}

	@Override
	public boolean isLeaf() {
		if(this.getNumIntervals()==0){
			return true;
		}
		return false;
	}
	
    public JPanel getView() {
		return new IntervalsPanel(this);
	}
}