package org.jhv.dataset.tree.views;
/**
 * @author Freek Verstringe
 */
import org.jhv.dataset.tree.views.FixedHeightButton;
import javax.swing.JPanel;

import org.helioviewer.jhv.layers.LayerDescriptor;
import org.jhv.dataset.tree.models.DatasetLayerTreeModel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.TreeMap;

public class IntervalPanel extends JPanel{
	private static final long serialVersionUID = 4342443227686604174L;
	
	private FixedHeightButton buttonLeft;
	private FixedHeightButton buttonCenter;
	private FixedHeightButton buttonRight;
	
	private JPanel typePanelContainer;
	private ArrayList<JPanel> typePanels;
	
	public IntervalPanel(TreeMap<String, ArrayList<LayerDescriptor> > types) {
		super();
		setLayout(new BorderLayout(0, 0));
		
		buttonLeft = new FixedHeightButton("l");
		FixedHeightButton spacerLeft = new FixedHeightButton("s");
		JPanel left = new JPanel();
		left.setLayout(new FlowLayout());
		left.add(spacerLeft);
		left.add(buttonLeft);
		
		add(left, BorderLayout.WEST);
		
		buttonCenter = new FixedHeightButton("c");
		add(buttonCenter, BorderLayout.CENTER);
		
		buttonRight = new FixedHeightButton("r");
		add(buttonRight, BorderLayout.EAST);
		
		typePanelContainer = new JPanel();
		typePanelContainer.setLayout(new GridLayout(types.size(),1));

		add(typePanelContainer, BorderLayout.SOUTH);
		
		typePanels = new ArrayList<JPanel> ();
		updatePanel(types);
	}
	
	public void addTypePanel(JPanel typePanel){
		this.typePanels.add(typePanel);
		typePanelContainer.add(typePanel);
	}
	
	public void updatePanel(TreeMap<String, ArrayList<LayerDescriptor> > types){
		for( ArrayList<LayerDescriptor> type : types.values()){
			TypePanel typePanel = new TypePanel(type);
			this.addTypePanel(typePanel);			
		}
	}	
}

