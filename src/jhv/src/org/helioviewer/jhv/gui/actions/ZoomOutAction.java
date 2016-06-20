package org.helioviewer.jhv.gui.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.input.KeyShortcuts;

@SuppressWarnings("serial")
public class ZoomOutAction extends AbstractAction {

    /**
     * @param small
     *            - if true, chooses a small (16x16), otherwise a large (24x24)
     *            icon for the action
     */
    public ZoomOutAction(boolean small, boolean useIcon) {
        super("Zoom Out", useIcon ? (small ? IconBank.getIcon(JHVIcon.ZOOM_OUT_SMALL) : IconBank.getIcon(JHVIcon.ZOOM_OUT)) : null);
        putValue(SHORT_DESCRIPTION, "Zoom out");

        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), true);
        putValue(ACCELERATOR_KEY, key);
        KeyShortcuts.registerKey(key, this);
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        Displayer.getCamera().zoom(+1 * Displayer.CAMERA_ZOOM_MULTIPLIER_BUTTON);
        Displayer.render(1);
    }

}
