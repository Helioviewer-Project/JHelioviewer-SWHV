package org.helioviewer.jhv.gui.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.View;

/**
 * Action to zoom such that the active layer fits completely in the viewport
 */
@SuppressWarnings("serial")
public class ZoomOneToOneAction extends AbstractAction {

    /**
     * @param small
     *            - if true, chooses a small (16x16), otherwise a large (24x24)
     *            icon for the action
     */
    public ZoomOneToOneAction(boolean small, boolean useIcon) {
        super("Actual Size", useIcon ? (small ? IconBank.getIcon(JHVIcon.ZOOM_1TO1_SMALL) : IconBank.getIcon(JHVIcon.ZOOM_1TO1)) : null);
        putValue(SHORT_DESCRIPTION, "Zoom to native resolution");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_0, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        View view = Layers.getActiveView();
        if (view != null) {
            MetaData m;
            ImageData d = view.getImageLayer().getImageData();
            if (d == null) // not yet decoded
                m = view.getMetaData(new JHVDate(0));
            else
                m = d.getMetaData();

            double imageFraction = Displayer.getActiveViewport().height / (double) m.getPixelHeight();

            Camera camera = Displayer.getCamera();
            double fov = 2. * Math.atan2(0.5 * m.getPhysicalRegion().height * imageFraction, camera.getViewpoint().distance);
            camera.setCameraFOV(fov);

            Displayer.render(1);
        }
    }

}
