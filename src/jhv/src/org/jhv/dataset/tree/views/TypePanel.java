package org.jhv.dataset.tree.views;
/**
 * @author Freek Verstringe
 */

import org.jhv.dataset.tree.views.FixedHeightButton;
import javax.swing.JPanel;


import java.awt.BorderLayout;
import java.awt.FlowLayout;


public class TypePanel extends JPanel{
	private static final long serialVersionUID = 8669761869598533103L;
	
	private FixedHeightButton buttonLeft;
	private FixedHeightButton buttonCenter;
	private FixedHeightButton buttonRight;
	
	public TypePanel() {
		super();
		setLayout(new BorderLayout(0, 0));
		
		buttonLeft = new FixedHeightButton("l");
		add(buttonLeft, BorderLayout.WEST);
		FixedHeightButton spacerLeft = new FixedHeightButton("s");
		FixedHeightButton spacerLeft2 = new FixedHeightButton("s");

		JPanel left = new JPanel();
		left.setLayout(new FlowLayout());
		left.add(spacerLeft);
		left.add(spacerLeft2);
		left.add(buttonLeft);
		add(left, BorderLayout.WEST);

		
		buttonCenter = new FixedHeightButton("c");
		add(buttonCenter, BorderLayout.CENTER);
		
		buttonRight = new FixedHeightButton("l");
		add(buttonRight, BorderLayout.EAST);


	}
	
}
