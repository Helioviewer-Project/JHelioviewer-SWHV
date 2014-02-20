package org.helioviewer.gl3d.gui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.camera.GL3DInteraction;

/**
 * Sets the current {@link GL3DInteraction} of the current {@link GL3DCamera} to
 * Rotation.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DSetRotationInteractionAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    public GL3DSetRotationInteractionAction() {
        super("Rotate");
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
        GL3DCameraSelectorModel.getInstance().getSelectedItem().setCurrentInteraction(GL3DCameraSelectorModel.getInstance().getSelectedItem().getRotateInteraction());
    }

}
