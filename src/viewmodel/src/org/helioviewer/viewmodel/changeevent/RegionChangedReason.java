package org.helioviewer.viewmodel.changeevent;

import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.View;

/**
 * Class represents a change reason when the region has changed.
 * 
 * @author Stephan Pagel
 * */
public final class RegionChangedReason implements ChangedReason {

    // memorizes the associated view
    private View view;

    // memorizes the new region
    private Region region;

    /**
     * Default constructor
     * 
     * @param aView
     *            View which caused the change reason.
     * @param aNewRegion
     *            New defined region.
     * */
    public RegionChangedReason(View aView, Region aNewRegion) {
        // memorize view
        view = aView;
        region = aNewRegion;
    }

    /**
     * {@inheritDoc}
     */
    public View getView() {
        return view;
    }

    /**
     * Returns the new region which was defined when this change reason
     * occurred.
     * 
     * @return new region.
     * */
    public Region getNewRegion() {
        return region;
    }

}
