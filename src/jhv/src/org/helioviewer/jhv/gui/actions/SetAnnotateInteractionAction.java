package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.camera.GL3DCamera;
import org.helioviewer.jhv.display.Displayer;

/**
 * Sets the interaction of the current camera to annotate
 */
@SuppressWarnings("serial")
public class SetAnnotateInteractionAction extends AbstractAction {

    public SetAnnotateInteractionAction() {
        super("Annotate");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        GL3DCamera cam = Displayer.getViewport().getCamera();
        cam.setCurrentInteraction(cam.getAnnotateInteraction());
    }

}
