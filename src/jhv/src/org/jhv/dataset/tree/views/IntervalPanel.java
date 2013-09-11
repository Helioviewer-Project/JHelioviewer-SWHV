package org.jhv.dataset.tree.views;
/**
 * @author Freek Verstringe
 */

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jhv.dataset.tree.models.DatasetInterval;

import javax.swing.BoxLayout;

public class IntervalPanel extends DatasetPanel{
	private static final long serialVersionUID = 4342443227686604174L;
	DatasetInterval model;
	public IntervalPanel(DatasetInterval model) {
		super();
		this.model = model;
		setLayout(new BoxLayout( this , BoxLayout.LINE_AXIS ));
		this.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
		this.setPreferredSize(new Dimension(250,100));

		this.setBackground(new Color((float)0.,(float)0.,(float)1.,(float)0.8));
		JLabel label = new JLabel(model.getTitle());
		Font currentFont = label.getFont();
		Font font = new Font(currentFont.getName(), currentFont.getStyle(), currentFont.getSize() + 10);
		label.setFont(font);

		//label.setOpaque(false);
		label.setBackground(new Color((float)1.,(float)0.,(float)1.,(float)0.5));
		//this.setBounds(0, 0, 100,100);

		add( label );
		
	}
	
}

