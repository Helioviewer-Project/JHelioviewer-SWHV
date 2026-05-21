package org.helioviewer.jhv.plugins.swek;

import org.helioviewer.jhv.events.JHVEvent;
import org.helioviewer.jhv.events.JHVEventParameter;

class SWEKData {

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
