/**
 *
 */
package org.helioviewer.jhv.data.container;

import java.util.Date;

import org.helioviewer.jhv.data.datatype.event.JHVEventType;

/**
 * A handler of JHV event requests should implement this interface and register
 * with the JHVEventContainer.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 */
public interface JHVEventContainerRequestHandler {

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
    public abstract void handleRequestForInterval(JHVEventType eventType, Date startDate, Date endDate);

}
