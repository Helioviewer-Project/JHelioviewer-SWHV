package org.helioviewer.jhv.gui;

import java.util.ArrayList;

import javax.swing.Timer;

import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.gui.components.base.BusyIndicator;
import org.helioviewer.jhv.gui.interfaces.LazyComponent;

public class UITimer {

    private static final ArrayList<LazyComponent> lazyComponents = new ArrayList<>();
    public static final BusyIndicator busyIndicator = new BusyIndicator();

    public static void start() {
        new Timer(1000 / 10, e -> action()).start();
    }

    public static void register(LazyComponent component) {
        if (!lazyComponents.contains(component))
            lazyComponents.add(component);
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

        lazyComponents.forEach(LazyComponent::lazyRepaint);
    }

}
