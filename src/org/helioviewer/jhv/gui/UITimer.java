package org.helioviewer.jhv.gui;

import java.util.ArrayList;

import javax.swing.Timer;

import org.helioviewer.jhv.gui.component.BusyIndicator;

public final class UITimer {

    private static final ArrayList<Interfaces.LazyComponent> lazyComponents = new ArrayList<>();

    public static void start() {
        new Timer(1000 / 10, e -> action()).start();
    }

    public static void register(Interfaces.LazyComponent component) {
        if (!lazyComponents.contains(component))
            lazyComponents.add(component);
    }

    private static void action() {
        BusyIndicator.incrementAngle();
        lazyComponents.forEach(Interfaces.LazyComponent::lazyRepaint);
    }

    private UITimer() {}
}
