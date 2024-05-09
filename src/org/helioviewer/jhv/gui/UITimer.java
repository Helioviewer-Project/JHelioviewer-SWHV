package org.helioviewer.jhv.gui;

import java.util.ArrayList;

import javax.swing.Timer;

import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.gui.components.base.BusyIndicator;

public class UITimer {

    private static final ArrayList<Interfaces.LazyComponent> lazyComponents = new ArrayList<>();
    public static final BusyIndicator busyIndicator = new BusyIndicator();

    public static void start() {
        new Timer(1000 / 10, e -> action()).start();
    }

    public static void register(Interfaces.LazyComponent component) {
        if (!lazyComponents.contains(component))
            lazyComponents.add(component);
    }

    private static volatile boolean completionChanged = false;

    // accessed from J2KReader threads
    public static void completionChanged() {
        completionChanged = true;
    }

    private static void action() {
        BusyIndicator.incrementAngle();

        if (completionChanged) {
            completionChanged = false;
            MoviePanel.getTimeSlider().repaint();
        }

        lazyComponents.forEach(Interfaces.LazyComponent::lazyRepaint);
    }

}
