package org.helioviewer.jhv.plugins.swek.download;

import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.config.SWEKParameter;

/**
 * Implemented by a listener interested in information coming from the filter
 * manager.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public interface FilterManagerListener {
    /**
     * Called if filters were added to the filter manager.
     * 
     * @param swekEventType
     *            the event type for which the filters where added
     */
    public abstract void filtersAdded(SWEKEventType swekEventType);

    /**
     * Called if the filters were removed from the filter manager.
     * 
     * @param parameter
     *            the parameter for which the filter was removed
     * @param swekEventType
     *            the event type for which the parameter was removed
     */
    public abstract void filtersRemoved(SWEKEventType swekEventType, SWEKParameter parameter);
}
