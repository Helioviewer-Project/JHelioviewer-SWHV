package org.helioviewer.jhv.data.container.cache;

import java.awt.Color;
import java.util.ArrayList;

import org.helioviewer.jhv.data.datatype.event.JHVEvent;

public class JHVRelatedEvents {
    private final ArrayList<JHVEvent> events = new ArrayList<JHVEvent>();
    private long start = Long.MAX_VALUE;
    private long end = Long.MIN_VALUE;
    private Color color = null;

    public JHVRelatedEvents(JHVEvent event) {
        super();
        this.add(event);
    }

    public ArrayList<JHVEvent> getEvents() {
        return events;
    }

    public void add(JHVEvent evt) {
        if (color == null) {
            color = evt.getColor();
        }
        long time = evt.getStartDate().getTime();
        if (time < start) {
            start = time;
        }
        time = evt.getEndDate().getTime();
        if (time > end) {
            end = time;
        }
        events.add(evt);
    }

    public long getEnd() {
        return end;
    }

    public long getStart() {
        return start;
    }

    public boolean isRelated(JHVEvent event) {
        return event.getJHVEventType() == events.get(0).getJHVEventType();
    }
}
