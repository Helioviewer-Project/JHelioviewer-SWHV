package org.helioviewer.jhv.plugins.swek;

import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.event.JHVEvent;
import org.helioviewer.jhv.event.JHVEventParameter;

class SWEKData {

    static final double CACTUS_START_RADIUS = 2.4;

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

    static double cactusDistance(JHVEvent evt, long timestamp) {
        return CACTUS_START_RADIUS + evt.getCMEParameters().speedKmPerSecond() * (timestamp - evt.start) / Sun.RadiusMeter;
    }

}
