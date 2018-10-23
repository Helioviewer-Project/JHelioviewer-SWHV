package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.layers.MovieDisplay;

@SuppressWarnings("serial")
public class ZoomFOVAnnotationAction extends AbstractAction {

    public ZoomFOVAnnotationAction() {
        super("Fit FOV Annotation");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JHVFrame.getAnnotateInteraction().zoom();
        MovieDisplay.render(1);
    }

}
