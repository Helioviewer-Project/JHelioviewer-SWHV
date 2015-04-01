package org.helioviewer.viewmodel.view;

import java.util.LinkedList;
import java.util.Vector;
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
 * <p>
 * It is possible to manage multiple sets of linked movies. Therefore, the
 * LinkedMovieManager manages multiple sets of itself, one per set of linked
 * movies. The default instance (id = 0) always exists and can not be deleted.
 *
 * @author Markus Langenberg
 */
public class LinkedMovieManager {

    private static Vector<LinkedMovieManager> instances = new Vector<LinkedMovieManager>();
    private static int activeInstance = 0;
    private final LinkedList<TimedMovieView> linkedMovies = new LinkedList<TimedMovieView>();
    private TimedMovieView masterView;
    private final Semaphore updateSemaphore = new Semaphore(1);
    private final Semaphore isPlayingSemaphore = new Semaphore(1);
    private final ReentrantLock isPlayingLock = new ReentrantLock();

    /**
     * Default constructor
     */
    private LinkedMovieManager() {
    }

    /**
     * Returns the active instance of the LinkedMovieManager.
     *
     * There can only be one instance active at a time, but it is possible to
     * manage multiple groups of linked movies by switching the active instance.
     *
     * @return The active instance of this class.
     * @see #setActiveInstance(int)
     * @see #createNewInstance()
     * @see #deleteInstance(int)
     */
    public static LinkedMovieManager getActiveInstance() {
        if (instances.isEmpty()) {
            instances.add(new LinkedMovieManager());
        }
        return instances.get(activeInstance);
    }

    /**
     * Sets the active instance of the LinkedMovieManager to use.
     *
     * @param instance
     *            ID of the new active instance.
     * @see #getActiveInstance()
     * @see #createNewInstance()
     * @see #deleteInstance(int)
     */
    public static void setActiveInstance(int instance) {
        if (instance < instances.size() && instances.get(instance) != null) {
            activeInstance = instance;
        }
    }

    /**
     * Creates a new instance and returns its id.
     *
     * The active instance will not change.
     *
     * @return Id of the new instance.
     * @see #getActiveInstance()
     * @see #setActiveInstance(int)
     * @see #deleteInstance(int)
     */
    public static int createNewInstance() {
        for (int i = 0; i < instances.size(); i++) {
            if (instances.get(i) == null) {
                instances.set(i, new LinkedMovieManager());
                return i;
            }
        }

        instances.add(new LinkedMovieManager());
        return instances.size() - 1;
    }

    /**
     * Deletes an existing instance.
     *
     * If the deleted instance was the active instance, sets the active instance
     * to the default instance (0).
     *
     * @param instance
     *            Id of instance to delete
     * @see #getActiveInstance()
     * @see #setActiveInstance(int)
     * @see #createNewInstance()
     */
    public static void deleteInstance(int instance) {
        if (instance != 0 && instance < instances.size() && instances.get(instance) != null) {
            LinkedList<TimedMovieView> linkedMovies = instances.get(instance).linkedMovies;
            while (!linkedMovies.isEmpty()) {
                linkedMovies.element().unlinkMovie();
            }

            instances.set(instance, null);
            if (activeInstance == instance) {
                setActiveInstance(0);
            }
        }
    }

    /**
     * Adds the given movie view to the set of linked movies.
     *
     * @param movieView
     *            View to add to the set of linked movies.
     */
    public synchronized void linkMovie(TimedMovieView movieView) {
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
    public synchronized void unlinkMovie(TimedMovieView movieView) {
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
     * Starts to play the set of linked movies.
     *
     * This function can be called directly from the movie view in its
     * playMovie()-function, hiding this functionality.
     *
     * <p>
     * Note, that this function will block recursive calls. The return value
     * indicates whether this function is already called.
     *
     * @return True, if the function was not called so far and therefore
     *         performed successful, false otherwise.
     */
    public synchronized boolean playLinkedMovies() {
        if (masterView == null)
            return true;

        if (updateSemaphore.tryAcquire()) {
            try {
                for (TimedMovieView movie : linkedMovies) {
                    movie.playMovie();
                }
            } finally {
                updateSemaphore.release();
            }
            return true;
        }
        return false;
    }

    /**
     * Stops to play the set of linked movies.
     *
     * This function can be called directly from the movie view in its
     * pauseMovie()-function, hiding this functionality.
     *
     * <p>
     * Note, that this function will block recursive calls. The return value
     * indicates whether this function is already called.
     *
     * @return True, if the function was not called so far and therefore
     *         performed successful, false otherwise.
     */
    public synchronized boolean pauseLinkedMovies() {
        if (masterView == null)
            return true;

        if (updateSemaphore.tryAcquire()) {
            try {
                masterView.pauseMovie();
            } finally {
                updateSemaphore.release();
            }
            return true;
        }
        return false;
    }

    /**
     * Updates all linked movies according to the current frame of the master
     * frame.
     *
     * This function can be called directly from the movie view in its
     * rendering-function, hiding this functionality.
     *
     * <p>
     * Note, that this function will block recursive calls. The return value
     * indicates whether this function is already called.
     *
     * @param event
     *            ChangeEvent to append new reasons to.
     */
    public synchronized void updateCurrentFrameToMaster() {
        if (masterView == null)
            return;

        if (updateSemaphore.tryAcquire()) {
            try {
                ImmutableDateTime masterTime = masterView.getCurrentFrameDateTime();

                for (TimedMovieView movieView : linkedMovies) {
                    if (movieView != masterView) {
                        movieView.setCurrentFrame(masterTime);
                    }
                }
            } finally {
                updateSemaphore.release();
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
    public synchronized boolean setCurrentFrame(ImmutableDateTime dateTime, boolean forceSignal) {
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
    private synchronized void updateMaster() {
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

        for (TimedMovieView movieView : linkedMovies) {
            lastAvailableFrame = 0;
            do {
                lastAvailableFrame = movieView.getMaximumFrameNumber();
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

            long interval = movieView.getFrameDateTime(lastAvailableFrame).getMillis() - movieView.getFrameDateTime(0).getMillis();
            interval /= (lastAvailableFrame + 1);

            if (interval < minimalInterval) {
                minimalInterval = interval;
                minimalIntervalView = movieView;
            }

            movieView.pauseMovie();
        }

        masterView = minimalIntervalView;
        if (isPlaying) {
            masterView.playMovie();
        }
    }

}
