package org.helioviewer.jhv.data.cache;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.data.event.JHVEventType;

public class JHVEventCacheResult {

    private final Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> availableEvents;
    private final Map<JHVEventType, List<Interval>> missingIntervals;

    public JHVEventCacheResult(Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> _availableEvents, Map<JHVEventType, List<Interval>> _missingIntervals) {
        availableEvents = _availableEvents;
        missingIntervals = _missingIntervals;
    }

    public Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> getAvailableEvents() {
        return availableEvents;
    }

    public Map<JHVEventType, List<Interval>> getMissingIntervals() {
        return missingIntervals;
    }

}
