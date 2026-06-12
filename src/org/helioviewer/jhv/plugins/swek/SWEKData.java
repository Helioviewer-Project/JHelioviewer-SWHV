package org.helioviewer.jhv.plugins.swek;

import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.event.JHVEvent;
import org.helioviewer.jhv.event.JHVEventParameter;

class SWEKData {

    static List<String> visibleParameterLines(JHVEvent evt) {
        List<String> lines = new ArrayList<>();
        for (JHVEventParameter p : evt.getSimpleVisibleEventParameters()) {
            String name = p.getParameterName();
            if (name != "event_description" && name != "event_title") { // interned
                lines.add(p.getParameterDisplayName() + " : " + p.getSimpleDisplayParameterValue());
            }
        }
        return lines;
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
