package org.jhv.dataset.tree.views;
/**
 * @author Freek Verstringe
 */
import org.jhv.dataset.tree.views.FixedHeightButton;
import javax.swing.JPanel;

import org.helioviewer.jhv.layers.LayerDescriptor;
import org.jhv.dataset.tree.actions.IntervalListener;
import org.jhv.dataset.tree.models.DatasetInterval;
import org.jhv.dataset.tree.models.DatasetLayerTreeModel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.TreeMap;

public class IntervalPanel extends JPanel implements IntervalListener{
	private static final long serialVersionUID = 4342443227686604174L;
	
	private FixedHeightButton buttonLeft;
	private FixedHeightButton buttonCenter;
	private FixedHeightButton buttonRight;
	
	private JPanel typePanelContainer;
	private ArrayList<JPanel> typePanels;
	DatasetInterval model;
	
	public IntervalPanel( DatasetInterval model) {
		super();
		this.model = model;
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
		
		typePanels = new ArrayList<JPanel> ();
		typePanelContainer = new JPanel();
		typePanelContainer.setLayout(new GridLayout(typePanels.size(),1));

		add(typePanelContainer, BorderLayout.SOUTH);
		
	}
	
	public void addTypePanel(JPanel typePanel){
		this.typePanels.add(typePanel);
		typePanelContainer.add(typePanel);
	}


	@Override
	public void typeInserted(int idx) {
		TypePanel typePanel = new TypePanel(this.model.getType(idx));
		this.typePanels.add(idx, typePanel);		
	}

	@Override
	public void typeRemoved(int idx) {
		
	}	
}

