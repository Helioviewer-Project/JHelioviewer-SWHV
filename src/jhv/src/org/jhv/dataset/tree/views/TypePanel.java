package org.jhv.dataset.tree.views;
/**
 * @author Freek Verstringe
 */

import org.jhv.dataset.tree.models.DatasetType;
import org.jhv.dataset.tree.views.FixedHeightButton;

import javax.swing.JLabel;
import javax.swing.JPanel;


import java.awt.BorderLayout;
import java.awt.FlowLayout;


public class TypePanel extends DatasetPanel{
	private static final long serialVersionUID = 8669761869598533103L;
	
	private DatasetType model;
	
	public TypePanel(DatasetType model) {
		super();
		this.model = model;
		setLayout(new BorderLayout());
		
		JLabel label = new JLabel(model.getTitle());
		add( label, BorderLayout.CENTER );


	}
	
}
