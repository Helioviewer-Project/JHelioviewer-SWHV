package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.renderable.components.RenderableImageLayer;

/**
 * Action to toggle visibility of off-limb corona
 */
@SuppressWarnings("serial")
public class ToggleCoronaVisibilityAction extends AbstractAction {

    public ToggleCoronaVisibilityAction() {
        super("Corona");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        RenderableImageLayer.toggleCorona();
        Displayer.display();
    }

}
