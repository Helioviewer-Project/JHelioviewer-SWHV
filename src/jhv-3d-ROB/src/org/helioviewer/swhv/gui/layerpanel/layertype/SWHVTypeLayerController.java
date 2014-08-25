package org.helioviewer.swhv.gui.layerpanel.layertype;

import org.helioviewer.swhv.gui.layerpanel.SWHVLayerController;
import org.helioviewer.swhv.mvc.SWHVModel;

public class SWHVTypeLayerController implements SWHVLayerController{
	private SWHVTypeLayerModel model;
	private SWHVTypeLayerPanel panel;
	
	public SWHVTypeLayerController(SWHVTypeLayerModel model, SWHVTypeLayerPanel panel){
		this.setModel(model);
		this.setView(panel);
		panel.setController(this);
		model.setController(this);
	}
	
	public SWHVModel getModel() {
		return (SWHVModel) model;
	}

	public void setModel(SWHVTypeLayerModel model) {
		this.model = model;
	}

	public SWHVTypeLayerPanel getPanel() {
		return panel;
	}

	public void setView(SWHVTypeLayerPanel panel) {
		this.panel = panel;
	}
	
	public void updateModel(){
	}
	
	public void remove(){
		this.model.remove();
	}

	public void setActive() {
		this.model.setActive(true, true);
	}	

	public void toggleFold() {
		this.model.toggleFold();		
	}
	
}
