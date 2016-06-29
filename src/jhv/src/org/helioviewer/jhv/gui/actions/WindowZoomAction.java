package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JFrame;

import org.helioviewer.jhv.gui.ImageViewerGui;

@SuppressWarnings("serial")
public class WindowZoomAction extends AbstractAction {

    public WindowZoomAction() {
        super("Zoom");
        putValue(SHORT_DESCRIPTION, "Zoom window");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int state = ImageViewerGui.getMainFrame().getExtendedState();
        state ^= JFrame.MAXIMIZED_BOTH;
        ImageViewerGui.getMainFrame().setExtendedState(state);
    }

}
