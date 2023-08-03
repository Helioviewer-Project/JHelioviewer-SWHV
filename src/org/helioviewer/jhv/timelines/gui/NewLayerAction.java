package org.helioviewer.jhv.timelines.gui;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.input.KeyShortcuts;
import org.helioviewer.jhv.timelines.Timelines;

@SuppressWarnings("serial")
public final class NewLayerAction extends AbstractAction {

    public NewLayerAction() {
        super("New Timeline...");

        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_N, UIGlobals.menuShortcutMask | InputEvent.ALT_DOWN_MASK);
        putValue(ACCELERATOR_KEY, key);
        KeyShortcuts.registerKey(key, this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Timelines.td.showDialog();
    }

}
