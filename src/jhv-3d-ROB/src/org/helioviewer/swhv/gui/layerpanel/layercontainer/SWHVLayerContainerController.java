package org.helioviewer.swhv.gui.layerpanel.layercontainer;

import org.helioviewer.globalstate.GlobalStateContainer;
import org.helioviewer.swhv.gui.layerpanel.daterangelayer.SWHVDateRangeLayerController;
import org.helioviewer.swhv.gui.layerpanel.daterangelayer.SWHVDateRangeLayerModel;
import org.helioviewer.swhv.gui.layerpanel.daterangelayer.SWHVDateRangeLayerPanel;
import org.helioviewer.swhv.mvc.SWHVController;

public class SWHVLayerContainerController implements SWHVController{
	
	private SWHVLayerContainerModel model;
	private SWHVLayerContainerPanel panel;

	public SWHVLayerContainerController(SWHVLayerContainerModel model, SWHVLayerContainerPanel panel){
		this.setModel(model);
		this.setPanel(panel);
		panel.setController(this);
	}
	
	public SWHVLayerContainerModel getSWHVLayerContainerModel(){
		return (SWHVLayerContainerModel)(this.getModel());
	}
	
	public SWHVLayerContainerPanel getSWHVLayerContainerPanel(){
		return this.getPanel();
	}

	public SWHVLayerContainerModel getModel() {
		return model;
	}

	public void setModel(SWHVLayerContainerModel model) {
		this.model = model;
	}

	public SWHVLayerContainerPanel getPanel() {
		return panel;
	}

	public void setPanel(SWHVLayerContainerPanel panel) {
		this.panel = panel;
	}
	
	public static void main(String [] args){
		SWHVLayerContainerPanel cp = new SWHVLayerContainerPanel();
		SWHVLayerContainerModel cm = GlobalStateContainer.getSingletonInstance().getLayerContainerModel();
		SWHVLayerContainerController cc = new SWHVLayerContainerController(cm, cp);
		SWHVDateRangeLayerModel drlm = new SWHVDateRangeLayerModel();
		SWHVDateRangeLayerPanel drlp = new SWHVDateRangeLayerPanel();
		SWHVDateRangeLayerController drlc = new SWHVDateRangeLayerController(drlm, drlp);
	}	
}
