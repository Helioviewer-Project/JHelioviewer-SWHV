package org.helioviewer.viewmodel.view;

import java.util.LinkedList;

import org.helioviewer.base.datetime.ImmutableDateTime;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;

/**
 * Class managing all linked movies.
 *
 * <p>
 * This class is responsible for synchronizing all linked movies. Therefore, all
 * linked movies have to call the various functions of this class. Then, all
 * other linked movies are set according to the new values.
 *
 * <p>
 * When playing a movie instead of just scrolling around, a master
 * movie is chosen, based on the average cadence of all linked movies. Only the
 * master movie is actually playing, all other movies are just set to the frame
 * closest to the one from the master movie.
 *
 * @author Markus Langenberg
 */
public class LinkedMovieManager {

    private static final LinkedList<MovieView> linkedMovies = new LinkedList<MovieView>();
    private static MovieView masterView;

    /**
     * Adds the given movie view to the set of linked movies.
     *
     * @param movieView
     *            View to add to the set of linked movies.
     */
    public static void linkMovie(MovieView movieView) {
        if (movieView.getMaximumFrameNumber() > 0 && !linkedMovies.contains(movieView)) {
            linkedMovies.add(movieView);
            updateMaster();
        }
    }

    /**
     * Removes the given movie view from the set of linked movies.
     *
     * @param movieView
     *            View to remove from the set of linked movies.
     */
    public static void unlinkMovie(MovieView movieView) {
        if (linkedMovies.contains(movieView)) {
            linkedMovies.remove(movieView);
            updateMaster();
        }
    }

    /**
     * Returns whether the given view is the master view.
     *
     * @param movieView
     *            View to test
     * @return True if the given view is the master view, false otherwise.
     */
    public static boolean isMaster(MovieView movieView) {
        if (movieView == null) {
            return false;
        } else {
            return (movieView == masterView);
        }
    }

    /**
     * Plays the set of linked movies.
     */
    public static void playLinkedMovies() {
        if (masterView instanceof JHVJPXView)
            ((JHVJPXView) masterView).playMovie();
    }

    /**
     * Pauses the set of linked movies.
     */
    public static void pauseLinkedMovies() {
        if (masterView instanceof JHVJPXView)
            ((JHVJPXView) masterView).pauseMovie();
    }

    /**
     * Updates all linked movies according to the current frame of the master
     * frame.
     */
    public static void updateCurrentFrameToMaster(View view) {
        if (masterView == null || view != masterView)
            return;

        ImmutableDateTime masterTime = masterView.getCurrentFrameDateTime();
        for (MovieView movieView : linkedMovies) {
            if (movieView != masterView) {
                movieView.setCurrentFrame(masterTime);
            }
        }
    }

    /**
     * Updates all linked movies according to the given time stamp.
     *
     * @param dateTime
     *            time which should be matched as close as possible
     */
    public static void setCurrentFrame(ImmutableDateTime dateTime) {
        for (MovieView movieView : linkedMovies) {
            movieView.setCurrentFrame(dateTime);
        }
    }

    public static void setCurrentFrame(MovieView view, int frameNumber) {
        frameNumber = Math.max(0, Math.min(view.getMaximumFrameNumber(), frameNumber));
        setCurrentFrame(view.getFrameDateTime(frameNumber));
    }

    /**
     * Recalculates the master view.
     *
     * The master view is the view whose movie is actually playing, whereas all
     * other movies just jump to the frame closest to the current frame from the
     * master panel.
     */
    private static void updateMaster() {
        masterView = null;

        if (linkedMovies.isEmpty()) {
            return;
        } else if (linkedMovies.size() == 1) {
            masterView = linkedMovies.element();
            return;
        }

        long minimalInterval = Long.MAX_VALUE;
        MovieView minimalIntervalView = null;

        for (MovieView movie : linkedMovies) {
            int lastAvailableFrame = movie.getMaximumFrameNumber();
            long interval = movie.getFrameDateTime(lastAvailableFrame).getMillis() - movie.getFrameDateTime(0).getMillis();
            interval /= (lastAvailableFrame + 1);

            if (interval < minimalInterval) {
                minimalInterval = interval;
                minimalIntervalView = movie;
            }
        }

        masterView = minimalIntervalView;
    }

}
