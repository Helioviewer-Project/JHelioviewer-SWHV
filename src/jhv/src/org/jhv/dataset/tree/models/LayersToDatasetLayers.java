package org.jhv.dataset.tree.models;

import java.util.ArrayList;

import org.helioviewer.jhv.layers.LayerDescriptor;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;

import org.helioviewer.viewmodel.view.View;

public class LayersToDatasetLayers implements LayersListener{
	
	public ArrayList<DatasetLayer> mappedNodes;
	public DatasetTreeModel treemodel;
    private final Object lock = new Object();
    
    private static LayersToDatasetLayers layersModel;
	
	public LayersToDatasetLayers(DatasetTreeModel treemodel){
		mappedNodes = new ArrayList<DatasetLayer>();
		this.treemodel = treemodel;
		layersModel = this;
	}
    public static LayersToDatasetLayers getSingletonInstance() {
        return layersModel;
    }
	
	@Override
	public void layerAdded(int idx) {
        synchronized (lock) {
        	this.treemodel.getTree().setEditable(false);
			LayerDescriptor descriptor = LayersModel.getSingletonInstance().getDescriptor(idx);
			if(idx==0){
				DatasetLayer node = treemodel.addLayerDescriptor(descriptor, 0);
				mappedNodes.add(0,node);
			}
        	this.treemodel.getTree().setEditable(true);
        }
	}

	@Override
	public void layerRemoved(View oldView, int oldIdx) {
        synchronized (lock) {
        	this.treemodel.getTree().setEditable(false);
			DatasetLayer node = mappedNodes.get(oldIdx);
			mappedNodes.remove(oldIdx);
			int index = node.getParent().getIndex(node);

			treemodel.removeLayerDescriptor(node.getDescriptor());
        	treemodel.getTree().setEditable(true);
        }
	}

	@Override
	public void layerChanged(int idx) {
        synchronized (lock) {
        	treemodel.getTree().setEditable(false);
        	LayerDescriptor desc = LayersModel.getSingletonInstance().getDescriptor(idx);
        	if(mappedNodes.size()>idx){
				mappedNodes.get(idx).setDescriptor(desc);
	        	DatasetLayer node = mappedNodes.get(idx);
	        	treemodel.changeLayerDescriptor(node.getDescriptor());
        	}
        	treemodel.getTree().setEditable(true);
        }
	}

	@Override
	public void activeLayerChanged(int idx) {
		
	}

	@Override
	public void viewportGeometryChanged() {
		
	}

	@Override
	public void timestampChanged(int idx) {
        synchronized (lock) {
        	LayerDescriptor desc = LayersModel.getSingletonInstance().getDescriptor(idx);
        	if(mappedNodes.size()>idx){
        		mappedNodes.get(idx).setDescriptor(desc);
        		DatasetLayer node = mappedNodes.get(idx);
        		//intervals.changeLayerDescriptor(node.getDescriptor());
        	}
        }		
	}

	@Override
	public void subImageDataChanged() {
		
	}

	@Override
	public void layerDownloaded(int idx) {
		
	}
	public View getView(LayerDescriptor descriptor) {
		int i=0;
		while(i<mappedNodes.size() &&  mappedNodes.get(i).getDescriptor() != descriptor){
			i++;
		}
		return LayersModel.getSingletonInstance().getLayer(i);
	}
	
	public int getIndex(LayerDescriptor descriptor) {
		int i=0;
		while(i<mappedNodes.size() &&  mappedNodes.get(i).getDescriptor() != descriptor){
			i++;
		}
		return i;
	}
	public void moveLayerDown(View view) {
        synchronized (lock) {        	

	    	int index = LayersModel.getSingletonInstance().findView(view);
			if(index<this.mappedNodes.size()-1){
					DatasetLayer layerToMove = this.mappedNodes.get(index);
			    	this.treemodel.removeLayerDescriptor(layerToMove.getDescriptor());
			    	this.treemodel.addLayerDescriptor(layerToMove.getDescriptor(), index+1);
					
			    	this.mappedNodes.remove(index);
					this.mappedNodes.add(index+1, layerToMove);
					LayersModel.getSingletonInstance().moveLayerDown(view);
				
	        }

        }
		for(int i=0; i<this.mappedNodes.size(); i++){
	    	this.layerChanged(i);
		}
	}
	public void moveLayerUp(View view) {
        synchronized (lock) {  
	    	int index = LayersModel.getSingletonInstance().findView(view);
	    	
			if(index>0){	      		
					DatasetLayer layerToMove = this.mappedNodes.get(index);
			    	this.treemodel.removeLayerDescriptor(layerToMove.getDescriptor());
			    	this.treemodel.addLayerDescriptor(layerToMove.getDescriptor(), index-1);
			    	
			    	this.mappedNodes.remove(index);
					this.mappedNodes.add(index-1, layerToMove);
					LayersModel.getSingletonInstance().moveLayerUp(view);				        
			}
        }
		for(int i=0; i<this.mappedNodes.size(); i++){
	    	this.layerChanged(i);
		}

	}

}
