package org.jhv.dataset.tree.views;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import org.jhv.dataset.tree.views.FixedHeightButton;
import javax.swing.JPanel;

import org.helioviewer.jhv.layers.LayerDescriptor;

public class LayerPanel extends JPanel{

	private static final long serialVersionUID = 7214631588320087038L;
	
	private FixedHeightButton buttonLeft;
	private FixedHeightButton buttonCenter;
	private FixedHeightButton buttonRight;

	
	public LayerPanel( LayerDescriptor descriptor ) {
		super();
		setLayout(new BorderLayout(0, 0));
		
		buttonLeft = new FixedHeightButton("l");
		add(buttonLeft, BorderLayout.WEST);
		FixedHeightButton spacerLeft = new FixedHeightButton("s");
		FixedHeightButton spacerLeft2 = new FixedHeightButton("s");
		FixedHeightButton spacerLeft3 = new FixedHeightButton("s");

		JPanel left = new JPanel();
		left.setLayout(new FlowLayout());
		left.add(spacerLeft);
		left.add(spacerLeft2);
		left.add(spacerLeft3);
		left.add(buttonLeft);
		add(left, BorderLayout.WEST);
		
		buttonCenter = new FixedHeightButton("c");
		add(buttonCenter, BorderLayout.CENTER);
		
		buttonRight = new FixedHeightButton("r");
		add(buttonRight, BorderLayout.EAST);

	}
	
}
