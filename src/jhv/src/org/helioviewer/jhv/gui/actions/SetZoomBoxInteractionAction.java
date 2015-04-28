package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.camera.GL3DCamera;
import org.helioviewer.jhv.display.Displayer;

/**
 * Sets the current {@link GL3DInteraction} of the current {@link GL3DCamera} to
 * Zoom Box Interaction.
 */
public class SetZoomBoxInteractionAction extends AbstractAction {

    public SetZoomBoxInteractionAction() {
        super("Zoom Box");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        GL3DCamera cam = Displayer.getActiveCamera();
        cam.setCurrentInteraction(cam.getZoomInteraction());
    }

}
