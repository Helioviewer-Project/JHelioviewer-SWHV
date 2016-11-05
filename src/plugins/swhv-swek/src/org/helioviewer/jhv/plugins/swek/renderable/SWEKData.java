package org.helioviewer.jhv.plugins.swek.renderable;

import java.util.ArrayList;
import java.util.Map;
import java.util.SortedMap;

import org.helioviewer.jhv.data.container.JHVEventHandler;
import org.helioviewer.jhv.data.container.cache.JHVEventCache;
import org.helioviewer.jhv.data.container.cache.JHVEventCache.SortedDateInterval;
import org.helioviewer.jhv.data.container.cache.JHVEventCacheResult;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventParameter;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.JHVRelatedEvents;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.TimespanListener;

public class SWEKData implements TimespanListener, JHVEventHandler {

    private static SWEKData instance;
    private long beginTime = Layers.getLastUpdatedTimestamp().milli;
    private long endTime = beginTime;

    private SWEKData() {
    }

    public static SWEKData getSingletonInstance() {
        if (instance == null) {
            instance = new SWEKData();
        }
        return instance;
    }

    public void reset() {
        instance = null;
    }

    public void requestEvents(boolean force) {
        long first = Layers.getStartDate().milli;
        long last = Layers.getEndDate().milli;
        if (force || first < beginTime || last > endTime) {
            beginTime = first;
            endTime = last;
            JHVEventCache.getSingletonInstance().requestForInterval(first, last, SWEKData.this);
        }
    }

    @Override
    public void timespanChanged(long start, long end) {
        requestEvents(false);
    }

    @Override
    public void newEventsReceived(Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> eventList) {
        // requestEvents(false);
        Displayer.display();
    }

    @Override
    public void cacheUpdated() {
        requestEvents(true);
    }

    public ArrayList<JHVRelatedEvents> getActiveEvents(long timestamp) {
        ArrayList<JHVRelatedEvents> activeEvents = new ArrayList<>();
        JHVEventCacheResult result = JHVEventCache.getSingletonInstance().get(beginTime, endTime, beginTime, endTime);
        Map<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> data = result.getAvailableEvents();
        for (Map.Entry<JHVEventType, SortedMap<SortedDateInterval, JHVRelatedEvents>> v1 : data.entrySet()) {
            for (Map.Entry<JHVEventCache.SortedDateInterval, JHVRelatedEvents> v2 : v1.getValue().entrySet()) {
                JHVRelatedEvents evr = v2.getValue();
                if (evr.getStart() <= timestamp && timestamp <= evr.getEnd())
                    activeEvents.add(evr);
            }
        }
        return activeEvents;
    }

    public static double readCMESpeed(JHVEvent evt) {
        double speed = 500;
        try {
            JHVEventParameter p = evt.getParameter("cme_radiallinvel");
            if (p != null)
                speed = Double.parseDouble(p.getParameterValue());
        } catch (Exception ignore) {
        }
        return speed;
    }

    public static double readCMEPrincipalAngleDegree(JHVEvent evt) {
        double principalAngle = 0;
        JHVEventParameter p = evt.getParameter("event_coord1");

        try {
            if (p != null)
                principalAngle = Double.parseDouble(p.getParameterValue()) + 90;
        } catch (Exception ignore) {
        }
        return principalAngle;
    }

    public static double readCMEAngularWidthDegree(JHVEvent evt) {
        double angularWidthDegree = 0;
        JHVEventParameter p = evt.getParameter("cme_angularwidth");
        try {
            if (p != null)
                angularWidthDegree = Double.parseDouble(p.getParameterValue());
        } catch (Exception ignore) {
        }
        return angularWidthDegree;
    }

}
