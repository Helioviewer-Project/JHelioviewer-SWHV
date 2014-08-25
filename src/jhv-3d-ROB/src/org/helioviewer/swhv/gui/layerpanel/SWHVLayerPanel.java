package org.helioviewer.swhv.gui.layerpanel;

import javax.swing.JPanel;

import org.helioviewer.swhv.mvc.SWHVPanel;

public abstract class SWHVLayerPanel extends JPanel implements SWHVPanel{
	
	public abstract SWHVLayerController getController();

}
