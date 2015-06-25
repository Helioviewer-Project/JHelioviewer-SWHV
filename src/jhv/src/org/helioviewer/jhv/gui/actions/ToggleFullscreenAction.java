package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.gui.ImageViewerGui;

/**
 * Action to switch fullscreen mode on and off
 * "fullscreen" means hiding the panel at left side of the window
 * 
 * @author Markus Langenberg
 */
@SuppressWarnings("serial")
public class ToggleFullscreenAction extends AbstractAction {

    public ToggleFullscreenAction() {
        super("Toggle fullscreen");
        putValue(SHORT_DESCRIPTION, "Toggle fullscreen");
        putValue(MNEMONIC_KEY, KeyEvent.VK_T);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.ALT_MASK));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ImageViewerGui.toggleShowSidePanel();
    }

}
