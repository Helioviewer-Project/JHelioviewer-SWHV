package org.helioviewer.viewmodel.changeevent;

import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.View;

/**
 * Class represents a change reason when the play-state of a (movie-) view has
 * changed.
 * 
 * @author Malte Nuhn
 * */
public class PlayStateChangedReason implements ChangedReason {

    // memorizes the associated view
    private View view;
    private LinkedMovieManager linkedMovieManager;
    private boolean isPlaying;

    private long id;

    private static long idCount = 0;

    /**
     * Default constructor
     * 
     * @param aView
     *            View which caused the change reason.
     * @param isPlaying
     *            The new play-state
     */
    public PlayStateChangedReason(View aView, LinkedMovieManager linkedMovieManager, boolean isPlaying) {

        // memorize view
        this.view = aView;
        this.linkedMovieManager = linkedMovieManager;
        this.isPlaying = isPlaying;

        id = idCount++;
    }

    /**
     * {@inheritDoc}
     */
    public View getView() {
        return view;
    }

    /**
     * Returns the new play-state which was defined when this change reason
     * occurred.
     * 
     * @return new play-state
     * */
    public boolean isPlaying() {
        return this.isPlaying;
    }

    /**
     * Returns the id which identifies this reason.
     * 
     * @return the id of the reason
     */
    public long getId() {
        return id;
    }

    /**
     * Return a reference to the LinkedMovieManager object that was used by the
     * view that changed its play-state, when the reason occured
     * 
     * @return LinkedMovieManager used by the view that changed its play-state
     */
    public LinkedMovieManager getLinkedMovieManager() {
        return this.linkedMovieManager;
    }
}
