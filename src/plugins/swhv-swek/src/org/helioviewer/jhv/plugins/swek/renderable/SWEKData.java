package org.helioviewer.jhv.plugins.swek.renderable;

import java.util.ArrayList;
import java.util.Map;
import java.util.SortedMap;

import org.helioviewer.jhv.data.cache.JHVEventCache;
import org.helioviewer.jhv.data.cache.JHVEventHandler;
import org.helioviewer.jhv.data.cache.JHVRelatedEvents;
import org.helioviewer.jhv.data.cache.SortedDateInterval;
import org.helioviewer.jhv.data.event.JHVEvent;
import org.helioviewer.jhv.data.event.JHVEventParameter;
import org.helioviewer.jhv.data.event.JHVEventType;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.TimespanListener;

public class SWEKData implements TimespanListener, JHVEventHandler {

    private static long startTime = Layers.getLastUpdatedTimestamp().milli;
    private static long endTime = startTime;

    private void requestEvents(boolean force, long start, long end) {
        if (force || start < startTime || end > endTime) {
            startTime = start;
            endTime = end;
            JHVEventCache.requestForInterval(start, end, this);
        }
    }

    @Override
    public void timespanChanged(long start, long end) {
        requestEvents(false, start, end);
    }

    @Override
    public void newEventsReceived() {
        Displayer.display();
    }

    @Override
    public void cacheUpdated() {
        requestEvents(true, Layers.getStartDate().milli, Layers.getEndDate().milli);
    }

    static ArrayList<JHVRelatedEvents> getActiveEvents(long timestamp) {
        ArrayList<JHVRelatedEvents> activeEvents = new ArrayList<>();

        Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> events = JHVEventCache.get(startTime, endTime, startTime, endTime).getAvailableEvents();
        for (SortedMap<SortedDateInterval, JHVRelatedEvents> eventMap : events.values()) {
            for (JHVRelatedEvents evr : eventMap.values()) {
                if (evr.getStart() <= timestamp && timestamp <= evr.getEnd()) {
                    activeEvents.add(evr);
                }
            }
        }
        return activeEvents;
    }

    static double readCMESpeed(JHVEvent evt) {
        JHVEventParameter p = evt.getParameter("cme_radiallinvel");
        try {
            if (p != null) {
                return Double.parseDouble(p.getParameterValue());
            }
        } catch (Exception ignore) {
        }
        return 500;
    }

    static double readCMEPrincipalAngleDegree(JHVEvent evt) {
        JHVEventParameter p = evt.getParameter("event_coord1");
        try {
            if (p != null) {
                return Double.parseDouble(p.getParameterValue()) + 90;
            }
        } catch (Exception ignore) {
        }
        return 0;
    }

    static double readCMEAngularWidthDegree(JHVEvent evt) {
        JHVEventParameter p = evt.getParameter("cme_angularwidth");
        try {
            if (p != null) {
                return Double.parseDouble(p.getParameterValue());
            }
        } catch (Exception ignore) {
        }
        return 0;
    }

}
