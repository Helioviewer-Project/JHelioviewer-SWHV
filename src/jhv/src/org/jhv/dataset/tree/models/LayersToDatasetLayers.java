package org.jhv.dataset.tree.models;

import java.util.ArrayList;

import org.helioviewer.jhv.layers.LayerDescriptor;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;

import org.helioviewer.viewmodel.view.View;

public class LayersToDatasetLayers implements LayersListener{
	
	public ArrayList<DatasetLayer> mappedNodes;
	public DatasetIntervals intervals;
	
	public LayersToDatasetLayers(DatasetIntervals intervals){
		mappedNodes = new ArrayList<DatasetLayer>();
		this.intervals = intervals;
	}
	
	@Override
	public void layerAdded(int idx) {
		LayerDescriptor descriptor = LayersModel.getSingletonInstance().getDescriptor(idx);
		if(idx==0){
			DatasetLayer node = intervals.addLayerDescriptor(descriptor, 0);
			mappedNodes.add(0,node);
		}
	}

	@Override
	public void layerRemoved(View oldView, int oldIdx) {
		DatasetLayer node = mappedNodes.get(oldIdx);
		mappedNodes.remove(oldIdx);
		int index = node.getParent().getIndex(node);
		intervals.removeLayerDescriptor(node.getDescriptor(), index);
	}

	@Override
	public void layerChanged(int idx) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void activeLayerChanged(int idx) {
		
	}

	@Override
	public void viewportGeometryChanged() {
		
	}

	@Override
	public void timestampChanged(int idx) {
		
	}

	@Override
	public void subImageDataChanged() {
		
	}

	@Override
	public void layerDownloaded(int idx) {
		
	}

}
