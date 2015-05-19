package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.camera.GL3DCamera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.view.AbstractView;

/**
 * Action to zoom such that the active layer fits completely in the viewport.
 */
@SuppressWarnings({"serial"})
public class ZoomOneToOneAction extends AbstractAction {

    /**
     * Constructor
     *
     * @param small
     *            - if true, chooses a small (16x16), otherwise a large (24x24)
     *            icon for the action
     */
    public ZoomOneToOneAction(boolean small, boolean useIcon) {
        super("Zoom 1:1", useIcon ? (small ? IconBank.getIcon(JHVIcon.ZOOM_1TO1_SMALL) : IconBank.getIcon(JHVIcon.ZOOM_1TO1)) : null);
        putValue(SHORT_DESCRIPTION, "Zoom to native resolution");
        putValue(MNEMONIC_KEY, KeyEvent.VK_Z);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.ALT_MASK));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        AbstractView view = LayersModel.getActiveView();
        if (view != null) {
            GL3DCamera camera = Displayer.getActiveCamera();
            double imageFraction = Displayer.getViewportHeight() / (double) ((HelioviewerMetaData) view.getMetaData()).getPixelHeight();
            double fov = 2. * Math.atan(-view.getMetaData().getPhysicalSize().y * imageFraction / 2. / camera.getTranslation().z);
            camera.setCameraFOV(fov);
            Displayer.display();
        }
    }

}
