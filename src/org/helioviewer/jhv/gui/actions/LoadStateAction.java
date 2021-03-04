package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.io.Load;

@SuppressWarnings("serial")
public class LoadStateAction extends AbstractAction {

    public LoadStateAction() {
        super("Load State...");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        File state = LoadStateDialog.get();
        if (state != null)
            Load.state.get(state.toURI());
    }

}
