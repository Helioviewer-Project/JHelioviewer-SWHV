package org.helioviewer.jhv.display;

import java.util.ArrayList;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;

import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventHighlightListener;

public class Displayer implements JHVEventHighlightListener {

    private final static Displayer instance = new Displayer();

    public static Displayer getSingletonInstance() {
        return instance;
    }

    private final ArrayList<DisplayListener> listeners = new ArrayList<DisplayListener>();
    private final ArrayList<RenderListener> renderListeners = new ArrayList<RenderListener>();

    private boolean displaying = false;

    private static boolean torender = false;
    private static boolean todisplay = false;
    private final MyListener timerListener = new MyListener();

    private Displayer() {
        Timer timer = new Timer(1000 / 20, timerListener);
        timer.start();
    }

    public static void render() {
        torender = true;
    }

    public static void display() {
        todisplay = true;
    }

    private class MyListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (todisplay == true) {
                todisplay = false;
                for (final DisplayListener listener : listeners) {
                    listener.display();
                }
            }

            if (torender == true) {
                torender = false;
                for (final RenderListener renderListener : renderListeners) {
                    renderListener.render();
                }
            }
        }
    }

    public void addListener(final DisplayListener listener) {
        listeners.add(listener);
    }

    public void removeListener(final DisplayListener listener) {
        listeners.remove(listener);
    }

    public void removeListeners() {
        listeners.clear();
    }

    public void addRenderListener(final RenderListener renderListener) {
        renderListeners.add(renderListener);
    }

    public void removeRenderListener(final RenderListener renderListener) {
        renderListeners.remove(renderListener);
    }

    @Override
    public void eventHightChanged(JHVEvent event) {
        this.display();
    }

}
