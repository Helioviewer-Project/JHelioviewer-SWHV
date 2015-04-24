package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;

public class ZoomOutAction extends AbstractAction {

    /**
     * Constructor
     *
     * @param small
     *            - if true, chooses a small (16x16), otherwise a large (24x24)
     *            icon for the action
     */
    public ZoomOutAction(boolean small) {
        super("Zoom out", small ? IconBank.getIcon(JHVIcon.ZOOM_OUT_SMALL) : IconBank.getIcon(JHVIcon.ZOOM_OUT));
        putValue(SHORT_DESCRIPTION, "Zoom out");
        putValue(MNEMONIC_KEY, KeyEvent.VK_O);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.ALT_MASK));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent arg0) {
        Displayer.getActiveCamera().zoom(+1);
    }

}
