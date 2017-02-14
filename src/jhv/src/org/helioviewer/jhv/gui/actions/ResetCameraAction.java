package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.Buttons;

@SuppressWarnings("serial")
public class ResetCameraAction extends AbstractAction {

    public ResetCameraAction() {
        super(Buttons.reset);
        putValue(SHORT_DESCRIPTION, "Reset camera position to default");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Displayer.getCamera().reset();
    }

}
