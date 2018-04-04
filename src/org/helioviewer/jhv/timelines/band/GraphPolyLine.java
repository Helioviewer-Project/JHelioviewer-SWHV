package org.helioviewer.jhv.timelines.band;

import java.util.List;

class GraphPolyline {

    public final int[] xPoints;
    public final int[] yPoints;

    GraphPolyline(List<Integer> dates, List<Integer> values) {
        int llen = dates.size();
        xPoints = new int[llen];
        yPoints = new int[llen];
        for (int j = 0; j < llen; j++) {
            xPoints[j] = dates.get(j);
            yPoints[j] = values.get(j);
        }
    }
}