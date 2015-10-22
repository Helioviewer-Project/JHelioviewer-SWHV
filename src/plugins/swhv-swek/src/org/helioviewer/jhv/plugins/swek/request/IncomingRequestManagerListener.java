package org.helioviewer.jhv.plugins.swek.request;

import java.util.Date;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;

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
     * New request for an interval was issued.
     *
     * @param eventType
     *
     * @param interval
     *            the interval for which the request was done
     */
    public abstract void newRequestForInterval(JHVEventType eventType, Interval<Date> interval);

}
