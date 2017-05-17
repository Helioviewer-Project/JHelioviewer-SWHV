package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.gui.ImageViewerGui;

@SuppressWarnings("serial")
public class LoadStateAction extends AbstractAction {

    public LoadStateAction() {
        super("Load state ...");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ImageViewerGui.getRenderableContainer().loadScene();
    }

}

