package org.helioviewer.viewmodel.view;

import java.util.LinkedList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

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

    private final LinkedList<TimedMovieView> linkedMovies = new LinkedList<TimedMovieView>();
    private TimedMovieView masterView;
    private final Semaphore updateSemaphore = new Semaphore(1);
    private final Semaphore isPlayingSemaphore = new Semaphore(1);
    private final ReentrantLock isPlayingLock = new ReentrantLock();

    /**
     * Adds the given movie view to the set of linked movies.
     *
     * @param movieView
     *            View to add to the set of linked movies.
     */
    public void linkMovie(TimedMovieView movieView) {
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
    public void unlinkMovie(TimedMovieView movieView) {
        if (linkedMovies.contains(movieView)) {
            linkedMovies.remove(movieView);
            updateMaster();

            if (!linkedMovies.isEmpty()) {
                movieView.pauseMovie();
            }
        }
    }

    /**
     * Returns, whether the given view is the master view.
     *
     * @param movieView
     *            View to test
     * @return True, if the given view is the master view, false otherwise.
     */
    public boolean isMaster(TimedMovieView movieView) {
        if (movieView == null) {
            return false;
        } else {
            return (movieView == masterView);
        }
    }

    /**
     * Returns the current master movie
     *
     * @return current master movie
     */
    public TimedMovieView getMasterMovie() {
        return masterView;
    }

    /**
     * Returns, whether the set of linked movies is playing.
     *
     * @return True, if the set of linked movies is playing, false otherwise.
     */
    public boolean isPlaying() {
        boolean isPlaying = false;

        try {
            isPlayingLock.lock();
            if (isPlayingSemaphore.tryAcquire()) {
                try {
                    isPlaying = (masterView != null && masterView.isMoviePlaying());
                } finally {
                    isPlayingSemaphore.release();
                }
            }
        } finally {
            isPlayingLock.unlock();
        }

        return isPlaying;
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
     *
     * This function can be called directly from the movie view in its
     * rendering-function, hiding this functionality.
     *
     */
    public void updateCurrentFrameToMaster() {
        if (masterView == null)
            return;

        ImmutableDateTime masterTime = masterView.getCurrentFrameDateTime();

        for (TimedMovieView movieView : linkedMovies) {
            if (movieView != masterView) {
                movieView.setCurrentFrame(masterTime);
            }
        }
    }

    /**
     * Updates all linked movies according to the given time stamp.
     *
     * This function can be called directly from the movie view, hiding this
     * functionality.
     *
     * <p>
     * Note, that this function will block recursive calls. The return value
     * indicates whether this function is already called.
     *
     * @param dateTime
     *            time which should be matches as close as possible
     * @param event
     *            ChangeEvent to append new reasons to.
     * @param forceSignal
     *            Forces a reader signal and depending on the reader mode a
     *            render signal regardless whether the frame changed
     * @return True, if the function was not called so far and therefore
     *         performed successful, false otherwise.
     */
    public boolean setCurrentFrame(ImmutableDateTime dateTime, boolean forceSignal) {
        if (updateSemaphore.tryAcquire()) {
            try {
                for (TimedMovieView movieView : linkedMovies) {
                    movieView.setCurrentFrame(dateTime, forceSignal);
                }
            } finally {
                updateSemaphore.release();
            }
            return true;
        }
        return false;
    }

    /**
     * Recalculates the master view.
     *
     * The master view is the view, whose movie is actually playing, whereas all
     * other movies just jump to the frame closest to the current frame from the
     * master panel.
     */
    private void updateMaster() {
        boolean isPlaying = (masterView != null && masterView.isMoviePlaying());
        masterView = null;

        if (linkedMovies.isEmpty()) {
            return;
        } else if (linkedMovies.size() == 1) {
            masterView = linkedMovies.element();
            if (isPlaying) {
                masterView.playMovie();
            }
            return;
        }

        long minimalInterval = Long.MAX_VALUE;
        TimedMovieView minimalIntervalView = null;
        int lastAvailableFrame = 0;

        for (TimedMovieView movie : linkedMovies) {
            lastAvailableFrame = 0;
            do {
                lastAvailableFrame = movie.getMaximumFrameNumber();
                if (lastAvailableFrame > 0) {
                    break;
                } else {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } while (true);

            long interval = movie.getFrameDateTime(lastAvailableFrame).getMillis() - movie.getFrameDateTime(0).getMillis();
            interval /= (lastAvailableFrame + 1);

            if (interval < minimalInterval) {
                minimalInterval = interval;
                minimalIntervalView = movie;
            }

            movie.pauseMovie();
        }

        masterView = minimalIntervalView;
        if (isPlaying) {
            masterView.playMovie();
        }
    }

}
