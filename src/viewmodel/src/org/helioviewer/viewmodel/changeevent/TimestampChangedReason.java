package org.helioviewer.viewmodel.changeevent;

import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

/**
 * Class represents a change reason when the timestamp of a layer has changed.
 * 
 * @author Markus Langenberg
 * */
public class TimestampChangedReason implements ChangedReason {

    // memorizes the associated view
    private View view;

    // memorizes the changed meta data
    private ImmutableDateTime dateTime;

    private long id;

    private static long idCount = 0;

    /**
     * Default constructor
     * 
     * @param aView
     *            View which caused the change reason.
     * @param newDateTime
     *            New time stamp object, which was changed.
     */
    public TimestampChangedReason(View aView, ImmutableDateTime newDateTime) {
        // memorize view
        view = aView;
        id = idCount++;
        dateTime = newDateTime;
    }

    /**
     * {@inheritDoc}
     */
    public View getView() {
        return view;
    }

    /**
     * Returns the new time stamp object which was defined when this change
     * reason occurred.
     * 
     * @return new time stamp object.
     * */
    public ImmutableDateTime getNewDateTime() {
        return dateTime;
    }

    /**
     * Returns the id which identifies this reason.
     * 
     * @return the id of the reason
     */
    public long getId() {
        return id;
    }

}
