/**
 *
 */
package org.helioviewer.jhv.data.container;

import java.util.Date;
import java.util.List;

import org.helioviewer.jhv.data.datatype.event.JHVEventType;

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
     */
    public abstract void handleRequestForDate(Date date);

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

    /**
     * Handle request for a list of dates
     *
     * @param dates
     *            the list of dates
     */
    public abstract void handleRequestForDateList(List<Date> dates);

}
