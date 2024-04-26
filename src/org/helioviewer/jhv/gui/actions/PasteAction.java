package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.gui.JHVTransferHandler;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.input.KeyShortcuts;

@SuppressWarnings("serial")
public final class PasteAction extends AbstractAction {

    public PasteAction() {
        super("Paste");

        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_V, UIGlobals.menuShortcutMask);
        putValue(ACCELERATOR_KEY, key);
        KeyShortcuts.registerKey(key, this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JHVTransferHandler.getInstance().readClipboard();
    }

}
