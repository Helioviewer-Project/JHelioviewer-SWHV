package org.helioviewer.viewmodel.changeevent;

import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.View;

/**
 * Class represents a change reason when the region was updated regardless
 * whether the region actually changed.
 * 
 * @author Andre Dau
 * */
public class RegionUpdatedReason implements ChangedReason {

    // ///////////////////////////////////////////////////////////////
    // Definitions
    // ///////////////////////////////////////////////////////////////

    // memorizes the associated view
    private View view;

    // memorizes the new region
    private Region region;

    private long id;

    private static int idCounter = 0;

    // ///////////////////////////////////////////////////////////////
    // Methods
    // ///////////////////////////////////////////////////////////////

    /**
     * Default constructor
     * 
     * @param aView
     *            View which caused the change reason.
     * @param anUpdatedRegion
     *            Updated region.
     * */
    public RegionUpdatedReason(View aView, Region anUpdatedRegion) {

        // memorize view
        view = aView;
        region = anUpdatedRegion;
        id = idCounter++;
    }

    /**
     * {@inheritDoc}
     */
    public View getView() {
        return view;
    }

    /**
     * Returns the updated region.
     * 
     * @return updated region.
     * */
    public Region getUpdatedRegion() {
        return region;
    }

    /**
     * Returns the id which identifies this reason
     * 
     * @return the id of the reason
     */
    public long getId() {
        return id;
    }
}