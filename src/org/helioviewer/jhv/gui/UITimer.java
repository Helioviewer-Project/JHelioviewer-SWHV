package org.helioviewer.jhv.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.gui.components.base.BusyIndicator;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.timelines.draw.DrawController;

public class UITimer {

    static {
        new Timer(1000 / 10, new UIListener()).start();
    }

    public static final BusyIndicator busyIndicator = new BusyIndicator();

    private static volatile boolean cacheChanged = false;

    // accessed from J2KReader threads
    public static void cacheStatusChanged() {
        cacheChanged = true;
    }

    private static class UIListener implements ActionListener {

        private int frameRate = -1;

        @Override
        public void actionPerformed(ActionEvent e) {
            BusyIndicator.incrementAngle();

            if (cacheChanged) {
                cacheChanged = false;
                MoviePanel.getTimeSlider().repaint();
            }
            MoviePanel.getTimeSlider().lazyRepaint();
            ImageViewerGui.getRenderableContainerPanel().lazyRepaint();

            int f = 0;
            if (Layers.isMoviePlaying()) {
                f = Layers.getActiveView().getCurrentFramerate();
            }

            if (f != frameRate) {
                frameRate = f;
                ImageViewerGui.getFramerateStatusPanel().update(f);
            }

            DrawController.draw();
        }

    }

}
