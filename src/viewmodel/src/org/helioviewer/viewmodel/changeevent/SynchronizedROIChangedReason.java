package org.helioviewer.viewmodel.changeevent;

import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.View;

/**
 * Class represents a change reason when the region of another view chain has
 * changed.
 * 
 * When at least one view chain is synchronized with another one it might be
 * important to transport the new region of the observed view chain through the
 * observer view chain for further computations.
 * 
 * @author Stephan Pagel
 * */
public class SynchronizedROIChangedReason implements ChangedReason {

    // ///////////////////////////////////////////////////////////////
    // Definitions
    // ///////////////////////////////////////////////////////////////

    // memorizes the associated view
    private View view;

    // region of other view chain
    private Region region;

    // ///////////////////////////////////////////////////////////////
    // Methods
    // ///////////////////////////////////////////////////////////////

    /**
     * Default constructor.
     * 
     * @param sender
     *            View which caused the change reason
     * @param regionToSynchronize
     *            Region from observed chain which should be synchronized.
     * */
    public SynchronizedROIChangedReason(View sender, Region regionToSynchronize) {

        // memorize parameter
        view = sender;
        region = regionToSynchronize;
    }

    /**
     * {@inheritDoc}
     */
    public View getView() {
        return view;
    }

    /**
     * Returns the changed region from observed view chain.
     * 
     * @return new region from observed view chain.
     * */
    public Region getRegionToSynchronize() {
        return region;
    }
}
