package org.helioviewer.swhv.mvc;

public class SWHVAbstractController implements SWHVController{
	private SWHVModel model;
	private SWHVPanel panel;
	
	@Override
	public SWHVModel getModel() {
		return model;
	}

	@Override
	public SWHVPanel getPanel() {
		// TODO Auto-generated method stub
		return panel;
	}
}
