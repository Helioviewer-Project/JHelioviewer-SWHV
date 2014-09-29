package org.helioviewer.swhv.gui.layerpanel.type;

import java.awt.event.ActionListener;

import javax.swing.ImageIcon;

public class SWHVChooseTypeModel implements SWHVTypeModel{
	private String type;
	private SWHVChooseTypeController controller;	
	private ActionListener actionListener;
	private ImageIcon typeIcon;

	public SWHVChooseTypeModel(String type, ActionListener layer, ImageIcon typeIcon){
		this.type = type;
		this.actionListener = layer;
		this.typeIcon = typeIcon;
	}
	
	public ActionListener getLayer() {
		return this.actionListener;
	}
	
	@Override
	public ImageIcon getIcon() {
		return typeIcon;
	}
	@Override
	public String getName() {
		return this.type;
	}
	
	public SWHVChooseTypeController getController() {
		return controller;
	}
	
	public ActionListener getActionListener() {
		return actionListener;
	}

	public void setController(SWHVChooseTypeController controller) {
		this.controller = controller;
	}
}
