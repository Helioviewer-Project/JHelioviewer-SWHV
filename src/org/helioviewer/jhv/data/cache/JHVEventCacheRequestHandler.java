package org.helioviewer.jhv.data.cache;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.data.event.JHVEventType;

// A handler of JHV event requests should implement this interface and register with the JHVEventContainer
public interface JHVEventCacheRequestHandler {

    void handleRequestForInterval(JHVEventType eventType, Interval interval);

}
