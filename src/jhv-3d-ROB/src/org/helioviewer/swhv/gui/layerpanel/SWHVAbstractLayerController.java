package org.helioviewer.swhv.gui.layerpanel;

import org.helioviewer.swhv.mvc.SWHVModel;

public class SWHVAbstractLayerController implements SWHVLayerController{
	private SWHVLayerModel model;
	private SWHVLayerPanel panel;
	
	@Override
	public void setActive() {
		this.model.setActive(true, true);
	}	
	@Override
	public void toggleFold() {
		this.model.toggleFold();		
	}

	@Override
	public SWHVModel getModel() {
		return model;
	}

	@Override
	public SWHVLayerPanel getPanel() {
		// TODO Auto-generated method stub
		return panel;
	}
}
