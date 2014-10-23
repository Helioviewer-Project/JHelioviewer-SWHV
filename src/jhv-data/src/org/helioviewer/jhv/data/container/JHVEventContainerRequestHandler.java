/**
 * 
 */
package org.helioviewer.jhv.data.container;

import java.util.Date;
import java.util.List;

/**
 * A handler of JHV event requests should implement this interface and register
 * with the JHVEventContainer.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 */
public interface JHVEventContainerRequestHandler {
    /**
     * Handle new request for a date.
     * 
     * @param date
     *            the date to handle the request for
     * @param requestID2
     */
    public abstract void handleRequestForDate(Date date, Long requestID2);

    /**
     * Handle request for an interval.
     * 
     * @param startDate
     *            the start date of the interval
     * @param endDate
     *            the end date of the interval
     * @param requestID
     */
    public abstract void handleRequestForInterval(Date startDate, Date endDate, Long requestID);

    /**
     * Handle request for a list of dates
     * 
     * @param dates
     *            the list of dates
     */
    public abstract void handleRequestForDateList(List<Date> dates, Long requestID);

    /**
     * 
     * 
     * @param requestID
     */
    public abstract void removeRequestID(Long requestID);
}
