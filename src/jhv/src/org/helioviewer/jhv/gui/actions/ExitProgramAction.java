package org.helioviewer.jhv.gui.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.ExitHooks;

@SuppressWarnings("serial")
public class ExitProgramAction extends AbstractAction {

    public ExitProgramAction() {
        super("Quit");
        putValue(SHORT_DESCRIPTION, "Quit program");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ExitHooks.exitProgram();
    }

}
