package org.helioviewer.viewmodel.view;

import java.util.LinkedList;

import org.helioviewer.base.datetime.ImmutableDateTime;

/**
 * Class managing all linked movies.
 *
 * <p>
 * This class is responsible for synchronizing all linked movies. Therefore, all
 * linked movies have to call the various functions of this class. Then, all
 * other linked movies are set according to the new values.
 *
 * <p>
 * When actually playing a movie instead of just scrolling around, a master
 * movie is chosen, based on the average cadence of all linked movies. Only the
 * master movie is actually playing, all other movies are just set to the frame
 * closest to the one from the master movie.
 *
 * @author Markus Langenberg
 */
public class LinkedMovieManager {

    private static final LinkedMovieManager instance = new LinkedMovieManager();

    public static LinkedMovieManager getSingletonInstance() {
        return instance;
    }

    private LinkedMovieManager() {
    }

    private final LinkedList<MovieView> linkedMovies = new LinkedList<MovieView>();
    private MovieView masterView;

    /**
     * Adds the given movie view to the set of linked movies.
     *
     * @param movieView
     *            View to add to the set of linked movies.
     */
    public void linkMovie(MovieView movieView) {
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
    public void unlinkMovie(MovieView movieView) {
        if (linkedMovies.contains(movieView)) {
            linkedMovies.remove(movieView);
            updateMaster();

            if (!linkedMovies.isEmpty()) {
                movieView.pauseMovie();
            }
        }
    }

    /**
     * Returns whether the given view is the master view.
     *
     * @param movieView
     *            View to test
     * @return True if the given view is the master view, false otherwise.
     */
    public boolean isMaster(MovieView movieView) {
        if (movieView == null) {
            return false;
        } else {
            return (movieView == masterView);
        }
    }

    /**
     * Plays the set of linked movies.
     */
    public void playLinkedMovies() {
        if (masterView != null)
            masterView.playMovie();
    }

    /**
     * Pauses the set of linked movies.
     */
    public void pauseLinkedMovies() {
        if (masterView != null)
            masterView.pauseMovie();
    }

    /**
     * Updates all linked movies according to the current frame of the master
     * frame.
     */
    public void updateCurrentFrameToMaster(MovieView view) {
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
     *            time which should be matches as close as possible
     * @param forceSignal
     *            Forces a reader signal and depending on the reader mode a
     *            render signal regardless whether the frame changed
     */
    public void setCurrentFrame(ImmutableDateTime dateTime, boolean forceSignal) {
        for (MovieView movieView : linkedMovies) {
            movieView.setCurrentFrame(dateTime, forceSignal);
        }
    }

    /**
     * Recalculates the master view.
     *
     * The master view is the view, whose movie is actually playing, whereas all
     * other movies just jump to the frame closest to the current frame from the
     * master panel.
     */
    private void updateMaster() {
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
