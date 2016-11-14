package org.helioviewer.jhv.gui.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.input.KeyShortcuts;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.View;

@SuppressWarnings("serial")
public class ZoomOneToOneAction extends AbstractAction {

    public ZoomOneToOneAction(boolean small, boolean useIcon) {
        super("Actual Size", useIcon ? IconBank.getIcon(small ? JHVIcon.ZOOM_1TO1_SMALL : JHVIcon.ZOOM_1TO1) : null);
        putValue(SHORT_DESCRIPTION, "Zoom to native resolution");

        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_0, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        putValue(ACCELERATOR_KEY, key);
        KeyShortcuts.registerKey(key, this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        View view = Layers.getActiveView();
        if (view != null) {
            Camera camera = Displayer.getCamera();
            MetaData m = view.getImageLayer().getMetaData();
            double imageFraction = Displayer.getActiveViewport().height / (double) m.getPixelHeight();
            double fov = 2. * Math.atan2(0.5 * m.getPhysicalRegion().height * imageFraction, camera.getViewpoint().distance);
            camera.setCameraFOV(fov);

            Displayer.render(1);
        }
    }

}
