package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.camera.GL3DCamera;
import org.helioviewer.jhv.display.Displayer;

/**
 * Sets the current {@link GL3DInteraction} of the current {@link GL3DCamera} to
 * Rotation.
 */
public class SetRotationInteractionAction extends AbstractAction {

    public SetRotationInteractionAction() {
        super("Rotate");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        GL3DCamera cam = Displayer.getActiveCamera();
        cam.setCurrentInteraction(cam.getRotateInteraction());
    }

}
