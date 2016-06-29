package org.helioviewer.jhv.gui.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.input.KeyShortcuts;

@SuppressWarnings("serial")
public class WindowMinimizeAction extends AbstractAction {

    public WindowMinimizeAction() {
        super("Minimize");
        putValue(SHORT_DESCRIPTION, "Minimize window");

        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_M, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        putValue(ACCELERATOR_KEY, key);
        KeyShortcuts.registerKey(key, this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int state = ImageViewerGui.getMainFrame().getExtendedState();
        state ^= JFrame.ICONIFIED;
        ImageViewerGui.getMainFrame().setExtendedState(state);
    }

}
