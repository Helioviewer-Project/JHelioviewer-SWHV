package org.helioviewer.swhv.gui.layerpanel.layercontainer;

import org.helioviewer.swhv.gui.layerpanel.SWHVLayerModel;
import org.helioviewer.swhv.gui.layerpanel.SWHVModelListener;

public interface SWHVLayerContainerModelListener extends SWHVModelListener{
	public void layerAdded(SWHVLayerContainerModel model, SWHVLayerModel layer, int i);
	public void layerAdded(SWHVLayerContainerModel model, SWHVLayerModel layer);		
	public void layerRemoved(SWHVLayerContainerModel model, int position);
	public void layerFolded(SWHVLayerContainerModel model);	
	public void layerActivated(int position);		
}
