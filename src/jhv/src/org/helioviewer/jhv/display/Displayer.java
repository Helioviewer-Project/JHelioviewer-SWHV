package org.helioviewer.jhv.display;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.HashSet;

import javax.swing.Timer;

import org.helioviewer.base.datetime.ImmutableDateTime;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventHighlightListener;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewDataHandler;

public class Displayer implements JHVEventHighlightListener {

    private static Component displayComponent;
    private static final HashSet<RenderListener> renderListeners = new HashSet<RenderListener>();

    private static int viewportWidth;
    private static int viewportHeight;

    public static void setViewportSize(int width, int height) {
        viewportWidth = width;
        viewportHeight = height;
    }

    public static int getViewportHeight() {
        return viewportHeight;
    }

    public static int getViewportWidth() {
        return viewportWidth;
    }

    private static boolean torender = false;
    private static boolean todisplay = false;

    private static final Timer displayTimer = new Timer(1000 / 20, new DisplayTimerListener());

    private Displayer() {
        displayTimer.start();
    }

    public static void render() {
        torender = true;
        todisplay = true;
    }

    public static void display() {
        todisplay = true;
    }

    private static class DisplayTimerListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (todisplay == true) {
                todisplay = false;
                displayComponent.repaint();
            }

            if (torender == true) {
                torender = false;
                for (final RenderListener renderListener : renderListeners) {
                    renderListener.render();
                }
            }
        }
    }

    public static final DisplayDataHandler displayDataHandler = new DisplayDataHandler();

    private static class DisplayDataHandler implements ViewDataHandler {

        @Override
        public void handleData(View view, ImageData imageData) {
            view.getImageLayer().setImageData(imageData);
            if (view == Layers.getActiveView()) {
                ImageViewerGui.getFramerateStatusPanel().updateFramerate(view.getActualFramerate());
            }
            ImageViewerGui.getRenderableContainer().fireTimeUpdated(view.getImageLayer());
            display();
        }

    }

    @Override
    public void eventHightChanged(JHVEvent event) {
        display();
    }

    public static void setDisplayComponent(Component component) {
        displayComponent = component;
    }

    public static void addRenderListener(final RenderListener renderListener) {
        renderListeners.add(renderListener);
    }

    public static void removeRenderListener(final RenderListener renderListener) {
        renderListeners.remove(renderListener);
    }

    private static final Displayer instance = new Displayer();

    public static Displayer getSingletonInstance() {
        return instance;
    }

}
