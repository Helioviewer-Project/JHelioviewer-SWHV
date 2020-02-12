package org.helioviewer.jhv.layers;

import javax.swing.Timer;

import org.helioviewer.jhv.events.JHVEventHighlightListener;
import org.helioviewer.jhv.events.JHVRelatedEvents;
import org.helioviewer.jhv.gui.JHVFrame;

public class MovieDisplay implements JHVEventHighlightListener {

    private static final MovieDisplay instance = new MovieDisplay();
    private static Timer displayTimer;

    public static void render(float decodeFactor) {
        if (ImageLayers.areEnabled())
            ImageLayers.decode(decodeFactor);
        else
            display();
    }

    public static void display() {
        displayTimer.restart();
    }

    @Override
    public void eventHightChanged() {
        display();
    }

    private MovieDisplay() {
        displayTimer = new Timer(10, e -> {
            displayTimer.stop();
            JHVFrame.getGLCanvas().display();
        });
        JHVRelatedEvents.addHighlightListener(this);
    }

}
