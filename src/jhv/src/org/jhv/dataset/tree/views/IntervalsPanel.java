package org.jhv.dataset.tree.views;


import org.jhv.dataset.tree.views.FixedHeightButton;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.actions.CloseAllLayersAction;
import org.helioviewer.jhv.gui.actions.ShowDialogAction;
import org.helioviewer.jhv.gui.dialogs.observation.ImageDataPanel;
import org.helioviewer.jhv.layers.LayerDescriptor;
import org.jhv.dataset.tree.actions.IntervalsListener;
import org.jhv.dataset.tree.models.DatasetIntervals;
import org.jhv.dataset.tree.models.DatasetLayerTreeModel;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import java.util.ArrayList;
import java.util.TreeMap;

/**
 * @author Freek Verstringe
 */
public class IntervalsPanel extends JPanel{

	private static final long serialVersionUID = -4980121173310259804L;
	
	
	DatasetIntervals model;
	
	public IntervalsPanel( DatasetIntervals model ) {
		super();
		this.model = model;
		setLayout(new BorderLayout());
				
		JLabel label = new JLabel("All layers");
		label.setOpaque(true);
		add(label, BorderLayout.CENTER);
		
	}		
}
