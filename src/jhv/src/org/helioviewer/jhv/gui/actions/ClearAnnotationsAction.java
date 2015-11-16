package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.ImageViewerGui;

/**
 * Action that resets the view transformation of the current camera
 * to its default settings
 */
@SuppressWarnings("serial")
public class ClearAnnotationsAction extends AbstractAction {

    public ClearAnnotationsAction(boolean small, boolean useIcon) {
        super("Clear Annotations", useIcon ? (IconBank.getIcon(JHVIcon.RESET)) : null);
        putValue(SHORT_DESCRIPTION, "Clear any annotation");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ImageViewerGui.getAnnotateInteraction().clear();
        Displayer.display();
    }

}
