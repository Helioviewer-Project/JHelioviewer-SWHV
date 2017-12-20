package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.layers.selector.State;

@SuppressWarnings("serial")
public class SaveStateAction extends AbstractAction {

    public SaveStateAction() {
        super("Save State");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        State.save();
    }

}

