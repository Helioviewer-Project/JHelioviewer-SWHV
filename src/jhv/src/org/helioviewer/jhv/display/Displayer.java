package org.helioviewer.jhv.display;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;

import javax.swing.Timer;

import org.helioviewer.base.datetime.ImmutableDateTime;
import org.helioviewer.jhv.camera.GL3DCamera;
import org.helioviewer.jhv.camera.GL3DObserverCamera;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventHighlightListener;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.AbstractView;
import org.helioviewer.viewmodel.view.MovieView;
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

        // sync linked movies to master
        if (view == masterView) {
            for (MovieView movieView : linkedMovies) {
                if (movieView != masterView) {
                    movieView.setCurrentFrame(dateTime);
                }
            }
            MoviePanel.setFrameSlider(masterView);
        }

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

    private static final LinkedList<MovieView> linkedMovies = new LinkedList<MovieView>();
    private static MovieView masterView;

    public static void setMasterMovie(AbstractView view) {
        boolean wasPlaying = masterView != null && masterView.isMoviePlaying();

        if (wasPlaying)
            pauseMovies();

        if (view instanceof MovieView) {
            masterView = (MovieView) view;
        } else
            masterView = null;

        MoviePanel.getSingletonInstance().setActiveMovie(masterView);

        if (wasPlaying)
            playMovies();
    }

    public static void playMovies() {
        if (masterView != null) {
            masterView.playMovie();
            MoviePanel.playStateChanged(true);
        }
    }

    public static void pauseMovies() {
        if (masterView != null) {
            masterView.pauseMovie();
            MoviePanel.playStateChanged(false);
        }
    }

    public static void setTime(ImmutableDateTime dateTime) {
        for (MovieView movieView : linkedMovies) {
            movieView.setCurrentFrame(dateTime);
        }
    }

    /**
     * Adds the given movie view to the set of linked movies.
     *
     * @param movieView
     *            View to add to the set of linked movies.
     */
    public static void linkMovie(AbstractView view) {
        if (!(view instanceof MovieView))
            return;

        MovieView movieView = (MovieView) view;
        if (!linkedMovies.contains(movieView)) {
            linkedMovies.add(movieView);
        }
    }

    /**
     * Removes the given movie view from the set of linked movies.
     *
     * @param movieView
     *            View to remove from the set of linked movies.
     */
    public static void unlinkMovie(AbstractView view) {
        if (!(view instanceof MovieView))
            return;

        MovieView movieView = (MovieView) view;
        if (linkedMovies.contains(movieView)) {
            linkedMovies.remove(movieView);
            movieView.pauseMovie();
        }
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
