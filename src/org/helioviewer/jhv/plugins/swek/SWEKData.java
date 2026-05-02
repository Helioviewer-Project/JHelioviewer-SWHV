package org.helioviewer.jhv.plugins.swek;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.helioviewer.jhv.events.JHVEvent;
import org.helioviewer.jhv.events.JHVEventCache;
import org.helioviewer.jhv.events.JHVEventParameter;
import org.helioviewer.jhv.events.JHVRelatedEvents;
import org.helioviewer.jhv.layers.Movie;

class SWEKData {

    static List<JHVRelatedEvents> getActiveEvents(long timestamp) {
        List<JHVRelatedEvents> events = JHVEventCache.getEvents(Movie.getStartTime(), Movie.getEndTime());
        if (events.isEmpty())
            return Collections.emptyList();

        List<JHVRelatedEvents> activeEvents = new ArrayList<>();
        for (JHVRelatedEvents evr : events) {
            if (evr.getStart() <= timestamp && timestamp <= evr.getEnd()) {
                activeEvents.add(evr);
            }
        }
        return activeEvents;
    }

    static double readCMESpeed(JHVEvent evt) {
        return readDouble(evt, "cme_radiallinvel", 500);
    }

    static double readCMEPrincipalAngleDegree(JHVEvent evt) {
        return readDouble(evt, "event_coord1", 0);
    }

    static double readCMEAngularWidthDegree(JHVEvent evt) {
        return readDouble(evt, "cme_angularwidth", 0);
    }

    private static double readDouble(JHVEvent evt, String parameter, double fallback) {
        JHVEventParameter p = evt.getParameter(parameter);
        if (p == null)
            return fallback;
        try {
            return Double.parseDouble(p.getParameterValue());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

}
