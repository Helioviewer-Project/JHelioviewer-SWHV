package org.jhv.dataset.tree.models;

import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;

public class DatasetNodePanel extends JPanel{
	JComponent [] components;
	
	DatasetNodePanel(JComponent [] components){
		super();
		this.components = components;
		
		this.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
		for(int i=0; i<components.length; i++){
			this.add(components[i]);
		}
	}
	
}
