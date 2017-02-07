package org.helioviewer.jhv.gui.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.ExitHooks;
import org.helioviewer.jhv.input.KeyShortcuts;

@SuppressWarnings("serial")
public class ExitProgramAction extends AbstractAction {

    public ExitProgramAction() {
        super("Quit");
        putValue(SHORT_DESCRIPTION, "Quit program");

        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        putValue(ACCELERATOR_KEY, key);
        KeyShortcuts.registerKey(key, this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (ExitHooks.exitProgram())
            System.exit(0);
    }

}
