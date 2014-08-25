package org.helioviewer.swhv.gui.layerpanel;

import org.helioviewer.swhv.mvc.SWHVController;

public interface SWHVLayerController extends SWHVController{
	public SWHVLayerPanel getPanel();
	void setActive();
	void toggleFold();		
}
