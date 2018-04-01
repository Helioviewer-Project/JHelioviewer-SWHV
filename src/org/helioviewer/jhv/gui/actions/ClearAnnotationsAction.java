package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.gui.ImageViewerGui;

@SuppressWarnings("serial")
public class ClearAnnotationsAction extends AbstractAction {

    public ClearAnnotationsAction() {
        super("Clear Annotations");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ImageViewerGui.getAnnotateInteraction().clear();
        Display.display();
    }

}
