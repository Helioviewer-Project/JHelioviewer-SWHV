package org.helioviewer.viewmodel.changeevent;

import org.helioviewer.viewmodel.view.View;

/**
 * Class represents a change reason when the cache status for remote movies has
 * changed.
 * 
 * There are two types of cache information: partial information and complete
 * information. Both types always assume, that the information within the
 * interval from the first frame to the frame given in the reason is the same.
 * There are no gaps.
 * 
 * @author Markus Langenberg
 * */
public class CacheStatusChangedReason implements ChangedReason {

    public enum CacheType {
        PARTIAL, COMPLETE
    };

    private View view;
    private CacheType type;
    private int value;

    /**
     * Default constructor.
     * 
     * @param aView
     *            View which caused the change reason.
     * @param aType
     *            Which cache information has changed: partial or complete
     * @param aValue
     *            Frame, to whom the new information reaches
     */
    public CacheStatusChangedReason(View aView, CacheType aType, int aValue) {
        view = aView;
        type = aType;
        value = aValue;
    }

    /**
     * {@inheritDoc}
     */
    public View getView() {
        return view;
    }

    /**
     * Returns the type of the information: PARTIAL or COMPLETE.
     * 
     * @return Type of the information: PARTIAL or COMPLETE
     */
    public CacheType getType() {
        return type;
    }

    /**
     * Returns the frame, to whom the new information reaches.
     * 
     * @return Frame, to whom the new information reaches
     */
    public int getValue() {
        return value;
    }

}
