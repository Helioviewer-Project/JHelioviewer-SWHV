package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.input.KeyShortcuts;

@SuppressWarnings("serial")
public final class WindowMinimizeAction extends AbstractAction {

    public WindowMinimizeAction() {
        super("Minimize");

        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_M, UIGlobals.menuShortcutMask);
        putValue(ACCELERATOR_KEY, key);
        KeyShortcuts.registerKey(key, this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int state = JHVFrame.getFrame().getExtendedState();
        state ^= JFrame.ICONIFIED;
        JHVFrame.getFrame().setExtendedState(state);
    }

}
