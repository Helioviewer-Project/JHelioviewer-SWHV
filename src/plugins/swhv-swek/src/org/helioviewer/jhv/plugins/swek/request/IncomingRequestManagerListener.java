package org.helioviewer.jhv.plugins.swek.request;

import java.util.Date;
import java.util.List;

import org.helioviewer.base.math.Interval;

/**
 * Interface needs to be implemented by every class interested in requests for
 * events for a certain date. This class handles the request coming from the
 * JHVEventContainer.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public interface IncomingRequestManagerListener {
    /**
     * New request for a date was issued
     * 
     * @param date
     *            the date for which the request was done
     */
    public abstract void newRequestForDate(Date date);

    /**
     * New request for an interval was issued.
     * 
     * @param interval
     *            the interval for which the request was done
     */
    public abstract void newRequestForInterval(Interval<Date> interval);

    /**
     * New request for a list of dates was issued.
     * 
     * @param dates
     *            the list of dates for which the request was done
     */
    public abstract void newRequestForDateList(List<Date> dates);
}
