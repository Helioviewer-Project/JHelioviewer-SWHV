package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.Layers;

@SuppressWarnings("serial")
public class ZoomInAction extends AbstractAction {

    /**
     * @param small
     *            - if true, chooses a small (16x16), otherwise a large (24x24)
     *            icon for the action
     */
    public ZoomInAction(boolean small, boolean useIcon) {
        super("Zoom in", useIcon ? (small ? IconBank.getIcon(JHVIcon.ZOOM_IN_SMALL) : IconBank.getIcon(JHVIcon.ZOOM_IN)) : null);
        putValue(SHORT_DESCRIPTION, "Zoom in");
        putValue(MNEMONIC_KEY, KeyEvent.VK_I);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, KeyEvent.ALT_MASK));
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        Layers.getActiveCamera().zoom(-1);
    }

}
