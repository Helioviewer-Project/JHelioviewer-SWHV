package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.dialogs.SoarDialog;
import org.helioviewer.jhv.input.KeyShortcuts;

@SuppressWarnings("serial")
public final class NewSoarAction extends AbstractAction {

    public NewSoarAction() {
        super("New SOAR Layer...");

        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_N, UIGlobals.menuShortcutMask | InputEvent.SHIFT_DOWN_MASK);
        putValue(ACCELERATOR_KEY, key);
        KeyShortcuts.registerKey(key, this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        SoarDialog.getInstance().showDialog();
    }

}
