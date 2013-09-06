package org.jhv.dataset.tree.views;


import org.jhv.dataset.tree.views.FixedHeightButton;
import javax.swing.JPanel;

import org.helioviewer.jhv.layers.LayerDescriptor;
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
	
	private JPanel intervalPanelContainer;
	private ArrayList<JPanel> intervalPanels;
	
	public IntervalsPanel( DatasetLayerTreeModel model ) {
		super();
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
		updatePanel(model);
	}
	
	public void addIntervalPanel(JPanel intervalPanel){
		this.intervalPanels.add(intervalPanel);
		intervalPanelContainer.add(intervalPanel);
	}
	
	public void updatePanel(DatasetLayerTreeModel model){
		for( TreeMap<String, ArrayList<LayerDescriptor> >interval  :model.getSortedLayers().values()){
			IntervalPanel intervalPanel = new IntervalPanel(interval);
			this.addIntervalPanel(intervalPanel);			
		}
	}	
}
