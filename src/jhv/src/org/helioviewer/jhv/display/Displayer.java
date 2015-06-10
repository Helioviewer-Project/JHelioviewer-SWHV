package org.helioviewer.jhv.display;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.HashSet;

import javax.swing.Timer;

import org.helioviewer.base.datetime.ImmutableDateTime;
import org.helioviewer.jhv.camera.GL3DCamera;
import org.helioviewer.jhv.camera.GL3DObserverCamera;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventHighlightListener;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;

public class Displayer implements JHVEventHighlightListener {

    private static DisplayListener displayListener;
    private static final HashSet<RenderListener> renderListeners = new HashSet<RenderListener>();
    private static final HashSet<TimeListener> timeListeners = new HashSet<TimeListener>();

    private static GL3DCamera activeCamera = new GL3DObserverCamera();
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

    public static void setActiveCamera(GL3DCamera camera) {
        activeCamera.deactivate();
        camera.activate(activeCamera);
        activeCamera = camera;
    }

    public static GL3DCamera getActiveCamera() {
        return activeCamera;
    }

    private static Date lastTimestamp;

    private static boolean torender = false;
    private static boolean todisplay = false;

    private final Timer timer = new Timer(1000 / 20, new MyListener());

    private Displayer() {
        timer.start();
    }

    public static void render() {
        torender = true;
        todisplay = true;
    }

    public static void display() {
        todisplay = true;
    }

    private class MyListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (todisplay == true) {
                todisplay = false;
                displayListener.display();
            }

            if (torender == true) {
                torender = false;
                for (final RenderListener renderListener : renderListeners) {
                    renderListener.render();
                }
            }
        }
    }

    public static void fireFrameChanged(JHVJP2View view, ImmutableDateTime dateTime) {
        ImageViewerGui.getRenderableContainer().fireTimeUpdated(view.getImageLayer());

        LayersModel.syncMaster(view, dateTime);

        if (view == LayersModel.getActiveView()) {
            ImageViewerGui.getFramerateStatusPanel().updateFramerate(view.getActualFramerate());

            lastTimestamp = dateTime.getTime();
            // fire TimeChanged
            activeCamera.timeChanged(lastTimestamp);
            for (final TimeListener listener : timeListeners) {
                listener.timeChanged(lastTimestamp);
            }
        }
        display();
    }

    public static Date getLastUpdatedTimestamp() {
        if (lastTimestamp == null) {
            lastTimestamp = LayersModel.getLastDate();
        }
        return lastTimestamp;
    }

    @Override
    public void eventHightChanged(JHVEvent event) {
        display();
    }

    public static void setDisplayListener(DisplayListener listener) {
        displayListener = listener;
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

    private static final Displayer instance = new Displayer();

    public static Displayer getSingletonInstance() {
        return instance;
    }

}
