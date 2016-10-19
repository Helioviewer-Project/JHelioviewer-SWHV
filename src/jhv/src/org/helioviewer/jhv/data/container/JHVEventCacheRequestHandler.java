package org.helioviewer.jhv.data.container;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;

/**
 * A handler of JHV event requests should implement this interface and register
 * with the JHVEventContainer.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 */
public interface JHVEventCacheRequestHandler {
    /**
     * Handle request for an interval.
     *
     * @param eventType
     *
     * @param startDate
     *            the start date of the interval
     * @param endDate
     *            the end date of the interval
     */
    void handleRequestForInterval(JHVEventType eventType, Interval interval);

}
