package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;

/**
 * Action that resets the view transformation of the current {@link GL3DCamera}
 * to its default settings.
 */
public class ResetCameraAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    public ResetCameraAction(boolean small) {
        super("Reset Camera", IconBank.getIcon(JHVIcon.RESET));
        putValue(SHORT_DESCRIPTION, "Reset Camera Position to Default");
        // putValue(MNEMONIC_KEY, KeyEvent.VK_R);
        // putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_COMMA,
        // KeyEvent.ALT_MASK));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        GL3DState.getActiveCamera().reset();
        Displayer.render();
    }

}
