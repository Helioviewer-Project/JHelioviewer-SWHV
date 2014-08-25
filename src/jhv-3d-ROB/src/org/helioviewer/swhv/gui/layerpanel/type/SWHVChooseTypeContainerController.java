package org.helioviewer.swhv.gui.layerpanel.type;

import org.helioviewer.swhv.mvc.SWHVController;

public class SWHVChooseTypeContainerController implements SWHVController{
	private SWHVChooseTypeContainerModel model;
	private SWHVChooseTypeContainerPanel panel;
	
	public SWHVChooseTypeContainerController(SWHVChooseTypeContainerModel model, SWHVChooseTypeContainerPanel panel){
		this.model = model;
		this.panel = panel;
		this.model.setController(this);
	}
	
	@Override
	public SWHVChooseTypeContainerModel getModel() {
		// TODO Auto-generated method stub
		return this.model;
	}

	@Override
	public SWHVChooseTypeContainerPanel getPanel() {
		// TODO Auto-generated method stub
		return this.panel;
	}

}
