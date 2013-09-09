package org.jhv.dataset.tree.views;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import org.jhv.dataset.tree.actions.RemoveLayerAction;
import org.jhv.dataset.tree.models.DatasetLayer;
import org.jhv.dataset.tree.views.FixedHeightButton;
import javax.swing.JPanel;

public class LayerPanel extends JPanel{

	private static final long serialVersionUID = 7214631588320087038L;
	
	private FixedHeightButton buttonLeft;
	private FixedHeightButton buttonCenter;
	private FixedHeightButton buttonRight;
	private DatasetLayer model;
	
	public LayerPanel( DatasetLayer model ) {
		super();
		this.model = model;
		setLayout(new BorderLayout(0, 0));
		
		buttonLeft = new FixedHeightButton(new RemoveLayerAction(0));
		add(buttonLeft, BorderLayout.WEST);
		
		buttonCenter = new FixedHeightButton("c");
		add(buttonCenter, BorderLayout.CENTER);
		
		buttonRight = new FixedHeightButton("r");
		add(buttonRight, BorderLayout.EAST);

	}
	
}
