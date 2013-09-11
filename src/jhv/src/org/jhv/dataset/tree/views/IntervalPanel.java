package org.jhv.dataset.tree.views;
/**
 * @author Freek Verstringe
 */
import org.jhv.dataset.tree.views.FixedHeightButton;

import javax.swing.JLabel;
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
	DatasetInterval model;
	public IntervalPanel(DatasetInterval model) {
		super();
		this.model = model;
		setLayout(new BorderLayout(0, 0));
		
		JLabel label = new JLabel(model.getTitle());
		label.setOpaque(true);
		add( label, BorderLayout.CENTER );
		
	}
	
}

