package org.helioviewer.swhv.gui.layerpanel.type;

public class SWHVChooseTypeController {
	
	private SWHVChooseTypePanel panel;
	private SWHVChooseTypeModel model;
	
	public SWHVChooseTypeController(SWHVChooseTypeModel model, SWHVChooseTypePanel panel){
		this.model = model;
		this.panel = panel;
		this.model.setController(this);
	}
	
	public SWHVChooseTypePanel getPanel(){
		return this.panel;
	}
	
	public SWHVChooseTypeModel getModel(){
		return this.model;
	}
	
}
