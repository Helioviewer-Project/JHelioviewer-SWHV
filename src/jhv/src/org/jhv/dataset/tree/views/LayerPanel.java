package org.jhv.dataset.tree.views;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import org.jhv.dataset.tree.models.DatasetLayer;
import org.jhv.dataset.tree.views.FixedHeightButton;

import javax.swing.JLabel;
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
		setLayout(new BorderLayout());
				
		JLabel label = new JLabel(this.model.getDescriptor().getType());
		add(label, BorderLayout.CENTER);


	}
	
}
