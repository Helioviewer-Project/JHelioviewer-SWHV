package org.helioviewer.jhv.data.container;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;

/**
 * A handler of JHV event requests should implement this interface and register
 * with the JHVEventContainer.
 */
public interface JHVEventCacheRequestHandler {

    void handleRequestForInterval(JHVEventType eventType, Interval interval);

}
