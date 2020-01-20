package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.input.KeyShortcuts;
import org.helioviewer.jhv.layers.selector.State;
import org.helioviewer.jhv.time.TimeUtils;

@SuppressWarnings("serial")
public class SaveStateAction extends AbstractAction {

    public SaveStateAction() {
        super("Save State");

        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_S, UIGlobals.menuShortcutMask);
        putValue(ACCELERATOR_KEY, key);
        KeyShortcuts.registerKey(key, this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        State.save(Settings.getProperty("path.state"), "state__" + TimeUtils.formatFilename(System.currentTimeMillis()) + ".jhv");
    }

}

