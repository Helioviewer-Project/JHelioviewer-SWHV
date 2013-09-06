package org.jhv.dataset.tree.views;
/**
 * @author Freek Verstringe
 */
import org.jhv.dataset.tree.actions.TypeListener;
import org.jhv.dataset.tree.models.DatasetType;
import org.jhv.dataset.tree.views.FixedHeightButton;
import javax.swing.JPanel;

import org.helioviewer.jhv.layers.LayerDescriptor;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;

public class TypePanel extends JPanel implements TypeListener{
	private static final long serialVersionUID = 8669761869598533103L;
	
	private FixedHeightButton buttonLeft;
	private FixedHeightButton buttonCenter;
	private FixedHeightButton buttonRight;
	
	private JPanel layerContainer;
	private ArrayList<JPanel> layerPanels;
	
	DatasetType model;
	
	public TypePanel(DatasetType model) {
		super();
		this.model = model;
		setLayout(new BorderLayout(0, 0));
		
		buttonLeft = new FixedHeightButton("l");
		add(buttonLeft, BorderLayout.WEST);
		FixedHeightButton spacerLeft = new FixedHeightButton("s");
		FixedHeightButton spacerLeft2 = new FixedHeightButton("s");

		JPanel left = new JPanel();
		left.setLayout(new FlowLayout());
		left.add(spacerLeft);
		left.add(spacerLeft2);
		left.add(buttonLeft);
		add(left, BorderLayout.WEST);

		
		buttonCenter = new FixedHeightButton("c");
		add(buttonCenter, BorderLayout.CENTER);
		
		buttonRight = new FixedHeightButton("l");
		add(buttonRight, BorderLayout.EAST);
		
		layerPanels = new ArrayList<JPanel>();

		layerContainer = new JPanel();
		layerContainer.setLayout(new GridLayout(layerPanels.size(),1));
		
		add(layerContainer, BorderLayout.SOUTH);

	}
	
	public void addLayerPanel(JPanel layer){
		this.layerPanels.add(layer);
		layerContainer.add(layer);
	}
	


	@Override
	public void layerInserted(int idx) {
		LayerPanel layerPanel = new LayerPanel(this.model.getLayer(idx));
		this.layerPanels.add(idx, layerPanel);

	}

	@Override
	public void layerRemoved(int idx) {
		this.layerPanels.remove(idx);
	}	
}
