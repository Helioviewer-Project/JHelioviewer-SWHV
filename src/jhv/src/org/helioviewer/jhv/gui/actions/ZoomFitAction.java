package org.helioviewer.jhv.gui.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.input.KeyShortcuts;

@SuppressWarnings("serial")
public class ZoomFitAction extends AbstractAction {

    public ZoomFitAction(boolean small, boolean useIcon) {
        super("Zoom to Fit", useIcon ? (small ? IconBank.getIcon(JHVIcon.ZOOM_FIT_SMALL) : IconBank.getIcon(JHVIcon.ZOOM_FIT)) : null);
        putValue(SHORT_DESCRIPTION, "Zoom to fit");

        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_9, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        putValue(ACCELERATOR_KEY, key);
        KeyShortcuts.registerKey(key, this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        CameraHelper.zoomToFit(Displayer.getCamera());
        Displayer.render(1);
    }

}
