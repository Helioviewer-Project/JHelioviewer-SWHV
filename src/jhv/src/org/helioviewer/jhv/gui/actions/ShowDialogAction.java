package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.gui.interfaces.ShowableDialog;

@SuppressWarnings("serial")
public class ShowDialogAction extends AbstractAction {

    private final ShowableDialog dialog;

    public ShowDialogAction(String name, ShowableDialog _dialog) {
        super(name);
        dialog = _dialog;
    }

    public void show() {
        actionPerformed(new ActionEvent("", 0, null));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        dialog.showDialog();
    }

}
