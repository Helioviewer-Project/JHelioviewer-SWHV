package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.input.KeyShortcuts;

@SuppressWarnings("serial")
public class ZoomOutAction extends AbstractAction {

    public ZoomOutAction() {
        super("Zoom Out");

        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, UIGlobals.menuShortcutMask);
        putValue(ACCELERATOR_KEY, key);
        KeyShortcuts.registerKey(key, this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Displayer.getCamera().zoom(+1 * Displayer.CAMERA_ZOOM_MULTIPLIER_BUTTON);
        Displayer.display();
    }

}
