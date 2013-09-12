package org.jhv.dataset.tree.views;
/**
 * @author Freek Verstringe
 */

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.View;
import org.jhv.dataset.tree.models.DatasetInterval;
import org.jhv.dataset.tree.models.LayersToDatasetLayers;

import javax.swing.BoxLayout;

public class IntervalPanel extends DatasetPanel{
	private static final long serialVersionUID = 4342443227686604174L;
	DatasetInterval model;
	public IntervalPanel(DatasetInterval model) {
		super();
		this.model = model;
		this.setPreferredSize(new Dimension(250, 19));
		this.setBorder(BorderFactory.createEmptyBorder(2,5,2,5)) ;
		setLayout(new BoxLayout( this , BoxLayout.LINE_AXIS ));

		JLabel label = new JLabel(model.getTitle());

		add( label );
		
		
	}
	
}

