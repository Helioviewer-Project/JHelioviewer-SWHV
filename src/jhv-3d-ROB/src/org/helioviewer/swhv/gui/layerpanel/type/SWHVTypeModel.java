package org.helioviewer.swhv.gui.layerpanel.type;

import java.awt.event.ActionListener;

import javax.swing.ImageIcon;

import org.helioviewer.swhv.gui.layerpanel.SWHVRegistrableLayerModel;

public interface SWHVTypeModel {
	public ActionListener getActionListener();
	public ImageIcon getIcon();
	public String getName();
}
