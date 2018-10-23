package org.helioviewer.jhv.layers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import org.helioviewer.jhv.events.JHVEventHighlightListener;
import org.helioviewer.jhv.events.JHVRelatedEvents;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.position.Position;

public class MovieDisplay implements ActionListener, JHVEventHighlightListener {

    private static final MovieDisplay instance = new MovieDisplay();
    private static final Timer timer = new Timer(1000 / 20, instance);

    static boolean isPlaying() {
        return timer.isRunning();
    }

    static void play() {
        timer.restart();
    }

    static void pause() {
        timer.stop();
    }

    static void setFPS(int fps) {
        timer.setDelay(1000 / fps);
    }

    static void handleData(Position viewpoint) { // sync between layers, special for ImageLayer.handleData
        if (ImageLayers.getSyncedImageLayers(viewpoint)) {
            JHVFrame.getGLWindow().display(); // asap
        }
    }

    public static void render(double decodeFactor) {
        if (ImageLayers.getNumEnabledImageLayers() == 0)
            display();
        else
            ImageLayers.decode(decodeFactor);
    }

    public static void display() {
        if (JHVFrame.getGLWindow() != null) //!
            JHVFrame.getGLWindow().display(); // asap
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Movie.advanceFrame();
    }

    @Override
    public void eventHightChanged() {
        display();
    }

    private MovieDisplay() {
        JHVRelatedEvents.addHighlightListener(this);
    }

}
