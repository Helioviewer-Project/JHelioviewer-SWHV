package org.helioviewer.jhv.layers;

import javax.swing.Timer;

import org.helioviewer.jhv.events.JHVEventHighlightListener;
import org.helioviewer.jhv.events.JHVRelatedEvents;
import org.helioviewer.jhv.gui.JHVFrame;

public class MovieDisplay implements JHVEventHighlightListener {

    private static final MovieDisplay instance = new MovieDisplay();
    private static final Timer movieTimer = new Timer(1000 / 20, e -> Movie.advanceFrame());
    private static Timer displayTimer;

    static boolean isPlaying() {
        return movieTimer.isRunning();
    }

    static void play() {
        movieTimer.restart();
    }

    static void pause() {
        movieTimer.stop();
    }

    static void setFPS(int fps) {
        movieTimer.setDelay(1000 / fps);
    }

    public static void render(double decodeFactor) {
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
        displayTimer = new Timer(1000 / 120, e -> JHVFrame.getGLWindow().display());
        displayTimer.setRepeats(false);
        JHVRelatedEvents.addHighlightListener(this);
    }

}
