package org.helioviewer.jhv.data.container.cache;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import org.helioviewer.base.interval.Interval;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;

public class JHVEventCacheResult {

    private final Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> availableEvents;
    private final Map<JHVEventType, List<Interval<Date>>> missingIntervals;
    private final List<Date> missingDates;

    public JHVEventCacheResult(Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> availableEvents, Map<JHVEventType, List<Interval<Date>>> missingIntervals, List<Date> missingDates) {
        this.availableEvents = availableEvents;
        this.missingDates = missingDates;
        this.missingIntervals = missingIntervals;
    }

    public Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> getAvailableEvents() {
        return availableEvents;
    }

    public Map<JHVEventType, List<Interval<Date>>> getMissingIntervals() {
        return missingIntervals;
    }

    public List<Date> getMissingDates() {
        return missingDates;
    }

}
