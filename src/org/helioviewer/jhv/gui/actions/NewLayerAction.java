package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.dialogs.ObservationDialog;
import org.helioviewer.jhv.input.KeyShortcuts;

@SuppressWarnings("serial")
public final class NewLayerAction extends AbstractAction {

    public NewLayerAction() {
        super("New Image Layer...");

        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_N, UIGlobals.menuShortcutMask);
        putValue(ACCELERATOR_KEY, key);
        KeyShortcuts.registerKey(key, this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ObservationDialog.getInstance().showDialog(true, null);
    }

}
