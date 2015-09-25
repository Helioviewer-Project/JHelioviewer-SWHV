package org.helioviewer.jhv.display;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import javax.swing.Timer;

import org.helioviewer.base.time.TimeUtils;
import org.helioviewer.jhv.camera.GL3DCamera;
import org.helioviewer.jhv.camera.GL3DObserverCamera;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventHighlightListener;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.renderable.viewport.GL3DViewport;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewDataHandler;

public class Displayer implements JHVEventHighlightListener {

    private static Component displayComponent;
    private static GL3DViewport miniview = new GL3DViewport(10, 10, 75, 75, new GL3DObserverCamera());

    public static GL3DViewport getMiniview() {
        return miniview;
    }

    private static GL3DViewport viewport = new GL3DViewport(0, 0, 100, 100, new GL3DObserverCamera());

    private static ArrayList<GL3DViewport> viewports = new ArrayList<GL3DViewport>() {
        {
            add(viewport);
        }
    };

    public static ArrayList<GL3DViewport> getViewports() {
        return viewports;
    }

    public static GL3DViewport getViewport() {
        return viewport;
    }

    public static void addViewport(GL3DViewport vp) {
        viewports.add(vp);
    }

    public static void removeViewport(GL3DViewport vp) {
        viewports.remove(vp);
    }

    private static boolean torender = false;
    private static boolean todisplay = false;

    private static final Timer displayTimer = new Timer(1000 / 20, new DisplayTimerListener());

    private Displayer() {
        displayTimer.start();
    }

    public static void render() {
        torender = true;
        // todisplay = true;
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

    public static void setActiveCamera(GL3DCamera camera) {
        GL3DCamera activeCamera = viewport.getCamera();
        activeCamera.deactivate();
        camera.activate(activeCamera);
        viewport.setCamera(camera);
    }

    private static Date lastTimestamp = TimeUtils.epoch.getDate();

    public static Date getLastUpdatedTimestamp() {
        return lastTimestamp;
    }

    public static final DisplayDataHandler displayDataHandler = new DisplayDataHandler();

    private static class DisplayDataHandler implements ViewDataHandler {

        @Override
        public void handleData(View view, ImageData imageData) {
            if (imageData == null) // null on load
                return;

            boolean timeChanged = false;

            if (view == Layers.getActiveView()) {
                Date timestamp = imageData.getMetaData().getDateObs().getDate();
                if (timestamp.getTime() != lastTimestamp.getTime()) {
                    timeChanged = true;
                    lastTimestamp = timestamp;
                }

                if (timeChanged) {
                    viewport.getCamera().timeChanged(lastTimestamp);
                    for (final TimeListener listener : timeListeners) {
                        listener.timeChanged(lastTimestamp);
                    }
                    ImageViewerGui.getFramerateStatusPanel().updateFramerate(view.getActualFramerate());
                }
            }
            view.getImageLayer().setImageData(imageData);
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

    private static final HashSet<RenderListener> renderListeners = new HashSet<RenderListener>();
    private static final HashSet<TimeListener> timeListeners = new HashSet<TimeListener>();

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
