package org.jhv.dataset.tree.views;


import org.jhv.dataset.tree.views.FixedHeightButton;
import javax.swing.JPanel;

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
	
	private FixedHeightButton buttonLeft;
	private FixedHeightButton buttonCenter;
	private FixedHeightButton buttonRight;
	
	DatasetIntervals model;
	
	public IntervalsPanel() {
		super();
		this.model = model;
		setLayout(new BorderLayout(0, 0));
		
		buttonLeft = new FixedHeightButton("l");
		add(buttonLeft, BorderLayout.WEST);
		
		buttonCenter = new FixedHeightButton("c");
		add(buttonCenter, BorderLayout.CENTER);
		
		buttonRight = new FixedHeightButton("r");
		add(buttonRight, BorderLayout.EAST);
		
	}		
}
