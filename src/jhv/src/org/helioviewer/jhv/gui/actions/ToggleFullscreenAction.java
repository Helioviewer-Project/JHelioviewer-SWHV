package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.gui.ImageViewerGui;

@SuppressWarnings("serial")
public class ToggleFullscreenAction extends AbstractAction {

    public ToggleFullscreenAction() {
        super("Toggle Side Panel");
        putValue(SHORT_DESCRIPTION, "Toggle side panel");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ImageViewerGui.toggleSidePanel();
    }

}
