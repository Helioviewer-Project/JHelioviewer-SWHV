package org.helioviewer.jhv.gui;

import java.util.HashSet;

import javax.swing.Timer;

import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.gui.components.base.BusyIndicator;
import org.helioviewer.jhv.gui.interfaces.LazyComponent;

public class UITimer {

    private static final HashSet<LazyComponent> lazyComponents = new HashSet<>();
    public static final BusyIndicator busyIndicator = new BusyIndicator();

    public static void start() {
        new Timer(1000 / 10, e -> action()).start();
    }

    public static void register(LazyComponent lazy) {
        lazyComponents.add(lazy);
    }

    public static void unregister(LazyComponent lazy) {
        lazyComponents.remove(lazy);
    }

    private static volatile boolean cacheChanged = false;

    // accessed from J2KReader threads
    public static void cacheStatusChanged() {
        cacheChanged = true;
    }

    private static void action() {
        BusyIndicator.incrementAngle();

        if (cacheChanged) {
            cacheChanged = false;
            MoviePanel.getTimeSlider().repaint();
        }

        for (LazyComponent lazy : lazyComponents)
            lazy.lazyRepaint();
    }

}
