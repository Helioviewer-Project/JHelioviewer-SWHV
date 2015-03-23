package org.helioviewer.jhv.display;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.Timer;

import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventHighlightListener;
import org.helioviewer.jhv.plugin.renderable.RenderableContainer;

public class Displayer implements JHVEventHighlightListener {

    private static final Displayer instance = new Displayer();
    private static final RenderableContainer renderableContainer = new RenderableContainer();

    public static Displayer getSingletonInstance() {
        return instance;
    }

    private static final ArrayList<DisplayListener> listeners = new ArrayList<DisplayListener>();
    private static final ArrayList<RenderListener> renderListeners = new ArrayList<RenderListener>();
    private static final ArrayList<TimeListener> timeListeners = new ArrayList<TimeListener>();

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
        @Override
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

    public static void addListener(final DisplayListener listener) {
        listeners.add(listener);
    }

    public static void removeListener(final DisplayListener listener) {
        listeners.remove(listener);
    }

    public static void removeListeners() {
        listeners.clear();
    }

    public static void addRenderListener(final RenderListener renderListener) {
        renderListeners.add(renderListener);
    }

    public static void removeRenderListener(final RenderListener renderListener) {
        renderListeners.remove(renderListener);
    }

    public static void addTimeListener(final TimeListener timeListener) {
        timeListeners.add(timeListener);
    }

    public static void removeTimeListener(final TimeListener timeListener) {
        timeListeners.remove(timeListener);
    }

    public static void fireTimeChanged(Date date) {
        for (final TimeListener listener : timeListeners) {
            listener.timeChanged(date);
        }
    }

    @Override
    public void eventHightChanged(JHVEvent event) {
        this.display();
    }

    public static RenderableContainer getRenderablecontainer() {
        return renderableContainer;
    }

}
