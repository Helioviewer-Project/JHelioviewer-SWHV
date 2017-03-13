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
import org.helioviewer.jhv.plugins.swek.SWEKPlugin;
import org.helioviewer.jhv.timelines.draw.DrawController;

public class SWEKData implements TimespanListener, JHVEventHandler {

    private static long beginTime = Layers.getLastUpdatedTimestamp().milli;
    private static long endTime = beginTime;

    public static void requestEvents(boolean force) {
        long first = Layers.getStartDate().milli;
        long last = Layers.getEndDate().milli;
        if (force || first < beginTime || last > endTime) {
            beginTime = first;
            endTime = last;
            JHVEventCache.requestForInterval(first, last, SWEKPlugin.swekData);
        }
    }

    @Override
    public void timespanChanged(long start, long end) {
        requestEvents(false);
    }

    @Override
    public void newEventsReceived() {
        Displayer.display();
        DrawController.drawRequest();
    }

    @Override
    public void cacheUpdated() {
        requestEvents(true);
    }

    public static ArrayList<JHVRelatedEvents> getActiveEvents(long timestamp) {
        ArrayList<JHVRelatedEvents> activeEvents = new ArrayList<>();

        Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> events = JHVEventCache.get(beginTime, endTime, beginTime, endTime).getAvailableEvents();
        for (SortedMap<SortedDateInterval, JHVRelatedEvents> eventMap : events.values()) {
            for (JHVRelatedEvents evr : eventMap.values()) {
                if (evr.getStart() <= timestamp && timestamp <= evr.getEnd()) {
                    activeEvents.add(evr);
                }
            }
        }
        return activeEvents;
    }

    public static double readCMESpeed(JHVEvent evt) {
        JHVEventParameter p = evt.getParameter("cme_radiallinvel");
        try {
            if (p != null) {
                return Double.parseDouble(p.getParameterValue());
            }
        } catch (Exception ignore) {
        }
        return 500;
    }

    public static double readCMEPrincipalAngleDegree(JHVEvent evt) {
        JHVEventParameter p = evt.getParameter("event_coord1");
        try {
            if (p != null) {
                return Double.parseDouble(p.getParameterValue()) + 90;
            }
        } catch (Exception ignore) {
        }
        return 0;
    }

    public static double readCMEAngularWidthDegree(JHVEvent evt) {
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
