package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.gl3d.GL3DImageLayer;
import org.helioviewer.jhv.display.Displayer;

/**
 * Action that toggle visibility of off-limb corona.
 */
public class ToggleCoronaVisibilityAction extends AbstractAction {

    public ToggleCoronaVisibilityAction() {
        super("Corona");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        GL3DImageLayer.toggleCorona();
        Displayer.display();
    }

}
