package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.gui.states.StateController;

public class View2DAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public View2DAction() {
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
        StateController.getInstance().set2DState();
    }
}
