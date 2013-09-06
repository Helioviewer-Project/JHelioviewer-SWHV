package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.gui.states.StateController;
import org.helioviewer.jhv.opengl.GLInfo;

public class View3DAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public View3DAction() {
        this.setEnabled(GLInfo.glIsEnabled() && GLInfo.glIsUsable());
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
        StateController.getInstance().set3DState();
    }
}
