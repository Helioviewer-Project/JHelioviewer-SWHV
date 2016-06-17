package org.helioviewer.jhv.gui.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.gui.ImageViewerGui;

@SuppressWarnings("serial")
public class ToggleFullscreenAction extends AbstractAction {

    public ToggleFullscreenAction() {
        super("Toggle Full Screen");
        putValue(SHORT_DESCRIPTION, "Toggle full screen");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ImageViewerGui.toggleFullScreen();
    }

}
