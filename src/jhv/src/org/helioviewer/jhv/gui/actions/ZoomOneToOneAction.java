package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.camera.GL3DCamera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.AbstractView;

/**
 * Action to zoom such that the active layer fits completely in the viewport.
 */
public class ZoomOneToOneAction extends AbstractAction {

    /**
     * Constructor
     *
     * @param small
     *            - if true, chooses a small (16x16), otherwise a large (24x24)
     *            icon for the action
     */
    public ZoomOneToOneAction(boolean small) {
        super("Zoom 1:1", small ? IconBank.getIcon(JHVIcon.ZOOM_1TO1_SMALL) : IconBank.getIcon(JHVIcon.ZOOM_1TO1));
        putValue(SHORT_DESCRIPTION, "Zoom to native resolution");
        putValue(MNEMONIC_KEY, KeyEvent.VK_Z);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.ALT_MASK));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        AbstractView view = Displayer.getLayersModel().getActiveView();
        GL3DCamera camera = Displayer.getActiveCamera();

        if (view != null) {
            Region region = view.getMetaData().getPhysicalRegion();
            double imheight = ((HelioviewerMetaData) view.getMetaData()).getPixelHeight();
            double imageFraction = Displayer.getViewportHeight() / imheight;
            if (region != null) {
                double fov = 2. * Math.atan(-region.getHeight() * imageFraction / 2. / camera.getTranslation().z);
                camera.setCameraFOV(fov);
                Displayer.display();
            }
        }
    }

}
