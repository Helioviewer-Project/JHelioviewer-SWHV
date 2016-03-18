package org.helioviewer.jhv.data.datatype.event;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Map;
import java.util.SortedMap;

import javax.swing.ImageIcon;

import org.helioviewer.jhv.data.container.cache.JHVCacheColors;
import org.helioviewer.jhv.data.container.cache.JHVEventCache.SortedDateInterval;

public class JHVRelatedEvents {
    private final ArrayList<JHVEvent> events = new ArrayList<JHVEvent>();
    private final SortedDateInterval interval = new SortedDateInterval(Long.MAX_VALUE, Long.MIN_VALUE);
    private final Color color;

    public JHVRelatedEvents(JHVEvent event, Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> eventsMap) {
        super();
        color = JHVCacheColors.getNextColor();
        this.add(event, eventsMap);
        eventsMap.get(event.getJHVEventType()).put(interval, this);
    }

    public ArrayList<JHVEvent> getEvents() {
        return events;
    }

    public void add(JHVEvent evt, Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> eventsMap) {

        long time = evt.getStartDate().getTime();
        if (time < interval.start) {
            interval.start = time;
        }
        time = evt.getEndDate().getTime();
        if (time > interval.end) {
            interval.end = time;
        }
        events.add(evt);
        eventsMap.get(evt.getJHVEventType()).remove(interval, this);
        eventsMap.get(evt.getJHVEventType()).put(interval, this);

    }

    public long getEnd() {
        return interval.end;
    }

    public long getStart() {
        return interval.start;
    }

    public Color getColor() {
        return color;
    }

    public boolean isRelated(JHVEvent event) {
        return event.getJHVEventType() == events.get(0).getJHVEventType();
    }

    public ImageIcon getIcon() {
        return events.get(0).getIcon();
    }

    public void merge(JHVRelatedEvents found) {
        this.events.addAll(found.getEvents());
    }

    public JHVEventType getJHVEventType() {
        return events.get(0).getJHVEventType();
    }
}
