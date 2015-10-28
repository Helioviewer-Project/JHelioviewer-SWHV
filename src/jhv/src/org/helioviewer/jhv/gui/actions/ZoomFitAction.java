package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;

/**
 * Action to zoom such that the active layer fits completely in the viewport
 */
@SuppressWarnings("serial")
public class ZoomFitAction extends AbstractAction {

    /**
     * @param small
     *            - if true, chooses a small (16x16), otherwise a large (24x24)
     *            icon for the action
     */
    public ZoomFitAction(boolean small, boolean useIcon) {
        super("Zoom to Fit", useIcon ? (small ? IconBank.getIcon(JHVIcon.ZOOM_FIT_SMALL) : IconBank.getIcon(JHVIcon.ZOOM_FIT)) : null);
        putValue(SHORT_DESCRIPTION, "Zoom to fit");
        putValue(MNEMONIC_KEY, KeyEvent.VK_F);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_K, KeyEvent.ALT_MASK));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Displayer.getViewport().getCamera().zoomToFit();
        Displayer.render();
    }

}
