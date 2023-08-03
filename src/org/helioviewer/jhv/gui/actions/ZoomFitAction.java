package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.input.KeyShortcuts;
import org.helioviewer.jhv.layers.MovieDisplay;

@SuppressWarnings("serial")
public final class ZoomFitAction extends AbstractAction {

    public ZoomFitAction() {
        super("Zoom to Fit");

        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_9, UIGlobals.menuShortcutMask);
        putValue(ACCELERATOR_KEY, key);
        KeyShortcuts.registerKey(key, this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        CameraHelper.zoomToFit(Display.getCamera());
        MovieDisplay.render(1);
    }

}
