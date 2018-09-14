package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.gui.JHVFrame;

@SuppressWarnings("serial")
public class ZoomFOVAnnotationAction extends AbstractAction {

    public ZoomFOVAnnotationAction() {
        super("Fit FOV Annotation");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JHVFrame.getAnnotateInteraction().zoom();
        Display.render(1);
    }

}
