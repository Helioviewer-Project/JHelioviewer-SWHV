package org.helioviewer.jhv.data.container.cache;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.data.container.cache.JHVEventCache.SortedDateInterval;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;

public class JHVEventCacheResult {

    private final Map<JHVEventType, SortedMap<SortedDateInterval, JHVEvent>> availableEvents;
    private final Map<JHVEventType, List<Interval<Date>>> missingIntervals;

    public JHVEventCacheResult(Map<JHVEventType, SortedMap<SortedDateInterval, JHVEvent>> availableEvents, Map<JHVEventType, List<Interval<Date>>> missingIntervals) {
        this.availableEvents = availableEvents;
        this.missingIntervals = missingIntervals;
    }

    public Map<JHVEventType, SortedMap<SortedDateInterval, JHVEvent>> getAvailableEvents() {
        return availableEvents;
    }

    public Map<JHVEventType, List<Interval<Date>>> getMissingIntervals() {
        return missingIntervals;
    }

}
