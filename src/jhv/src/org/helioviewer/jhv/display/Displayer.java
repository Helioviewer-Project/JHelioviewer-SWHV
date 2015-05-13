package org.helioviewer.jhv.display;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.Timer;

import org.helioviewer.base.datetime.ImmutableDateTime;
import org.helioviewer.jhv.camera.GL3DCamera;
import org.helioviewer.jhv.camera.GL3DObserverCamera;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventHighlightListener;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;

public class Displayer implements JHVEventHighlightListener {

    public static final ImmutableDateTime epochDateTime = ImmutableDateTime.parseDateTime("2000-01-01T00:00:00");

    private static DisplayListener displayListener;
    private static final ArrayList<RenderListener> renderListeners = new ArrayList<RenderListener>();
    private static final ArrayList<TimeListener> timeListeners = new ArrayList<TimeListener>();

    private static GL3DCamera activeCamera = new GL3DObserverCamera(true);
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

    public static void addFirstTimeListener(final TimeListener timeListener) {
        timeListeners.add(0, timeListener);
    }

    public static void removeTimeListener(final TimeListener timeListener) {
        timeListeners.remove(timeListener);
    }

    private static final LayersModel layersModel = new LayersModel();

    public static void fireFrameChanged(JHVJP2View view, ImmutableDateTime dateTime) {
        int idx = layersModel.findView(view);
        if (idx != -1 /* layersModel.isValidIndex(idx) */) {
            // update timestamp labels
            ImageViewerGui.getRenderableContainer().fireTimeUpdated(view.getImageLayer());

            if (idx == layersModel.getActiveLayer() && dateTime != null) {
                ImageViewerGui.getFramerateStatusPanel().updateFramerate(layersModel.getFPS(view));
                MoviePanel.setFrameSlider(view);

                lastTimestamp = dateTime.getTime();
                // fire TimeChanged
                for (final TimeListener listener : timeListeners) {
                    listener.timeChanged(lastTimestamp);
                }
            }
            display();
        }
    }

    public static Date getLastUpdatedTimestamp() {
        if (lastTimestamp == null) {
            Date lastDate = layersModel.getLastDate();
            if (lastDate != null) {
                lastTimestamp = layersModel.getLastDate();
                return lastTimestamp;
            }
            return null;
        } else {
            return lastTimestamp;
        }
    }

    @Override
    public void eventHightChanged(JHVEvent event) {
        Displayer.display();
    }

    public static LayersModel getLayersModel() {
        return layersModel;
    }

    private static final Displayer instance = new Displayer();

    public static Displayer getSingletonInstance() {
        return instance;
    }

}
