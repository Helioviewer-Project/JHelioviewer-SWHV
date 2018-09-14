package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JFrame;

import org.helioviewer.jhv.gui.JHVFrame;

@SuppressWarnings("serial")
public class WindowZoomAction extends AbstractAction {

    public WindowZoomAction() {
        super("Zoom");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int state = JHVFrame.getFrame().getExtendedState();
        state ^= JFrame.MAXIMIZED_BOTH;
        JHVFrame.getFrame().setExtendedState(state);
    }

}
