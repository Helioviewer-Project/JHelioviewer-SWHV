package org.helioviewer.jhv.data.cache;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.data.event.SWEKSupplier;

public class JHVEventCacheResult {

    private final Map<SWEKSupplier, SortedMap<SortedDateInterval, JHVRelatedEvents>> availableEvents;
    private final Map<SWEKSupplier, List<Interval>> missingIntervals;

    public JHVEventCacheResult(Map<SWEKSupplier, SortedMap<SortedDateInterval, JHVRelatedEvents>> _availableEvents, Map<SWEKSupplier, List<Interval>> _missingIntervals) {
        availableEvents = _availableEvents;
        missingIntervals = _missingIntervals;
    }

    public Map<SWEKSupplier, SortedMap<SortedDateInterval, JHVRelatedEvents>> getAvailableEvents() {
        return availableEvents;
    }

    public Map<SWEKSupplier, List<Interval>> getMissingIntervals() {
        return missingIntervals;
    }

}
