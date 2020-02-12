package org.helioviewer.jhv.layers;

import java.awt.EventQueue;

import org.helioviewer.jhv.events.JHVEventHighlightListener;
import org.helioviewer.jhv.events.JHVRelatedEvents;
import org.helioviewer.jhv.gui.JHVFrame;

public class MovieDisplay implements JHVEventHighlightListener {

    private static final MovieDisplay instance = new MovieDisplay();

    public static void render(float decodeFactor) {
        if (ImageLayers.areEnabled())
            ImageLayers.decode(decodeFactor);
        else
            display();
    }

    public static void display() {
        EventQueue.invokeLater(() -> JHVFrame.getGLCanvas().display()); // decouple from caller
    }

    @Override
    public void eventHightChanged() {
        display();
    }

    private MovieDisplay() {
        JHVRelatedEvents.addHighlightListener(this);
    }

}
