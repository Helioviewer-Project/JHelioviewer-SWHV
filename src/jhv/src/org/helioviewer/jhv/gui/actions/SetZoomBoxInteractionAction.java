package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.camera.GL3DInteraction;
import org.helioviewer.gl3d.scenegraph.GL3DState;

/**
 * Sets the current {@link GL3DInteraction} of the current {@link GL3DCamera} to
 * Zoom Box Interaction.
 */
public class SetZoomBoxInteractionAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    public SetZoomBoxInteractionAction() {
        super("Zoom Box");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        GL3DCamera cam = GL3DState.getActiveCamera();
        cam.setCurrentInteraction(cam.getZoomInteraction());
    }

}
