package org.helioviewer.jhv.data.container.cache;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.data.container.cache.JHVEventCache.SortedDateInterval;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.JHVRelatedEvents;

public class JHVEventCacheResult {

    private final Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> availableEvents;
    private final Map<JHVEventType, List<Interval>> missingIntervals;

    public JHVEventCacheResult(Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> availableEvents, Map<JHVEventType, List<Interval>> missingIntervals) {
        this.availableEvents = availableEvents;
        this.missingIntervals = missingIntervals;
    }

    public Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> getAvailableEvents() {
        return availableEvents;
    }

    public Map<JHVEventType, List<Interval>> getMissingIntervals() {
        return missingIntervals;
    }

}
