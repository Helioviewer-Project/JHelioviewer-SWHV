package org.helioviewer.jhv.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;

import javax.swing.Timer;

import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.gui.components.base.BusyIndicator;
import org.helioviewer.jhv.gui.interfaces.LazyComponent;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.timelines.draw.DrawController;

public class UITimer {

    static {
        new Timer(1000 / 10, new UIListener()).start();
    }

    private static final HashSet<LazyComponent> lazyComponents = new HashSet<>();

    public static void register(LazyComponent lazy) {
        lazyComponents.add(lazy);
    }

    public static void unregister(LazyComponent lazy) {
        lazyComponents.remove(lazy);
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

            for (LazyComponent lazy : lazyComponents)
                lazy.lazyRepaint();

            int f = 0;
            ImageLayer layer;
            if (Movie.isPlaying() && (layer = Layers.getActiveImageLayer()) != null) {
                f = layer.getView().getCurrentFramerate();
            }

            if (f != frameRate) {
                frameRate = f;
                ImageViewerGui.getFramerateStatusPanel().update(f);
            }

            DrawController.draw();
        }

    }

}
