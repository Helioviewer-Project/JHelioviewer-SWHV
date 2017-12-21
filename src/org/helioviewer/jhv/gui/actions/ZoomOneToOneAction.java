package org.helioviewer.jhv.gui.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.input.KeyShortcuts;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.LayersContainer;
import org.helioviewer.jhv.metadata.MetaData;

@SuppressWarnings("serial")
public class ZoomOneToOneAction extends AbstractAction {

    public ZoomOneToOneAction() {
        super("Actual Size");

        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_0, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        putValue(ACCELERATOR_KEY, key);
        KeyShortcuts.registerKey(key, this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MetaData m;
        ImageLayer layer = LayersContainer.getActiveLayer();
        if (layer != null && (m = layer.getMetaData()) != null) {
            Camera camera = Displayer.getCamera();
            double imageFraction = Displayer.getActiveViewport().height / (double) m.getPixelHeight();
            double fov = 2. * Math.atan2(0.5 * m.getPhysicalRegion().height * imageFraction, camera.getViewpoint().distance);
            camera.setFOV(fov);

            Displayer.render(1);
        }
    }

}
