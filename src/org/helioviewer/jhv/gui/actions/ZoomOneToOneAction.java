package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.input.KeyShortcuts;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.metadata.MetaData;

@SuppressWarnings("serial")
public final class ZoomOneToOneAction extends AbstractAction {

    public ZoomOneToOneAction() {
        super("Actual Size");

        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_0, UIGlobals.menuShortcutMask);
        putValue(ACCELERATOR_KEY, key);
        KeyShortcuts.registerKey(key, this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ImageLayer layer = Layers.getActiveImageLayer();
        if (layer != null) {
            MetaData m = layer.getMetaData();
            Camera camera = Display.getCamera();
            double imageFraction = Display.getActiveViewport().height / (double) m.getPixelHeight();
            double fov = 2. * Math.atan2(0.5 * m.getPhysicalRegion().height * imageFraction, camera.getViewpoint().distance);
            camera.setFOV(fov);

            MovieDisplay.render(1);
        }
    }

}
