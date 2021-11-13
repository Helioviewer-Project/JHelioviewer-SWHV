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
