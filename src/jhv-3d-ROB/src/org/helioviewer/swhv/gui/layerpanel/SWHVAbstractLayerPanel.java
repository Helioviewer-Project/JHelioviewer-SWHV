package org.helioviewer.swhv.gui.layerpanel;

import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.swhv.gui.GUISettings;


public abstract class SWHVAbstractLayerPanel extends SWHVLayerPanel implements SWHVLayerModelListener {
	private static final long serialVersionUID = 1L;
	protected JButton treeButton;
	protected JButton removeButton;
	protected JPanel spacer;
	public void setDimensions(){
    	setPreferredSize(new Dimension(GUISettings.GRIDPANELWIDTH, GUISettings.LAYERHEIGHT));
    	setMaximumSize(new Dimension(GUISettings.GRIDPANELWIDTH, GUISettings.LAYERHEIGHT));
    	setMinimumSize(new Dimension(GUISettings.GRIDPANELWIDTH, GUISettings.LAYERHEIGHT));		
	}
	public void addSpacer(){
    	spacer = new JPanel();
    	spacer.setPreferredSize(new Dimension(0, GUISettings.LAYERHEIGHT));
    	spacer.setMaximumSize(new Dimension(0, GUISettings.LAYERHEIGHT));
    	spacer.setMinimumSize(new Dimension(0, GUISettings.LAYERHEIGHT));
    	add(spacer);
	}
	
	public void addTreeButton(){
    	treeButton = new JButton();
    	treeButton.setIcon(IconBank.getIcon(JHVIcon.FORWARD));
    	treeButton.setContentAreaFilled(false);
    	treeButton.setBorderPainted(false);
    	treeButton.setBackground(null);
		add(treeButton);		
		treeButton.setPreferredSize(new Dimension(GUISettings.LAYERHEIGHT, GUISettings.LAYERHEIGHT));
    	treeButton.setMargin(new Insets(0, 0, 0, 0));
    	treeButton.setOpaque(true);
	}
	
	public void addRemoveButton(){
    	removeButton = new JButton();
    	removeButton.setIcon(IconBank.getIcon(JHVIcon.REMOVE_LAYER));
    	removeButton.setContentAreaFilled(false);
    	removeButton.setBorderPainted(false);
    	removeButton.setBackground(null);
		add(removeButton);
		removeButton.setPreferredSize(new Dimension(GUISettings.LAYERHEIGHT, GUISettings.LAYERHEIGHT));
    	removeButton.setMargin(new Insets(0, 0, 0, 0));
    	removeButton.setOpaque(true);		
	}
	
}
