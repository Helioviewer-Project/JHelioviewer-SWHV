package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.layers.MovieDisplay;

@SuppressWarnings("serial")
public class ClearAnnotationsAction extends AbstractAction {

    public ClearAnnotationsAction() {
        super("Clear Annotations");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JHVFrame.getInteraction().clearAnnotations();
        MovieDisplay.display();
    }

}
