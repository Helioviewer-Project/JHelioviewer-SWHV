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
public class IntervalsPanel extends JPanel implements IntervalsListener{

	private static final long serialVersionUID = -4980121173310259804L;
	
	private FixedHeightButton buttonLeft;
	private FixedHeightButton buttonCenter;
	private FixedHeightButton buttonRight;
	
	private JPanel intervalPanelContainer;
	private ArrayList<JPanel> intervalPanels;
	DatasetIntervals model;
	
	public IntervalsPanel(DatasetIntervals model) {
		super();
		this.model = model;
		setLayout(new BorderLayout(0, 0));
		
		buttonLeft = new FixedHeightButton("l");
		add(buttonLeft, BorderLayout.WEST);
		
		buttonCenter = new FixedHeightButton("c");
		add(buttonCenter, BorderLayout.CENTER);
		
		buttonRight = new FixedHeightButton("r");
		add(buttonRight, BorderLayout.EAST);
		
		intervalPanelContainer = new JPanel();
		add(intervalPanelContainer, BorderLayout.SOUTH);
		this.intervalPanelContainer.setLayout(new GridLayout(model.getNumIntervals(),1));
		intervalPanels = new ArrayList<JPanel> ();
		
	}
	
	public void addIntervalPanel(JPanel intervalPanel){
		this.intervalPanels.add(intervalPanel);
		intervalPanelContainer.add(intervalPanel);
	}
	

	@Override
	public void intervalInserted(int idx) {
		IntervalPanel intervalPanel = new IntervalPanel(this.model.getInterval(idx));
		this.intervalPanels.add(idx, intervalPanel);
	}

	@Override
	public void intervalRemoved(int idx) {
		this.intervalPanels.remove(idx);
	}	
}
