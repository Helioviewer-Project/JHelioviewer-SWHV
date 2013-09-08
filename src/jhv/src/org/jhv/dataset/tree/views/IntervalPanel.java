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

public class IntervalPanel extends JPanel{
	private static final long serialVersionUID = 4342443227686604174L;
	
	private FixedHeightButton buttonLeft;
	private FixedHeightButton buttonCenter;
	private FixedHeightButton buttonRight;
	
	
	public IntervalPanel() {
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
		
		
	}
	
}

