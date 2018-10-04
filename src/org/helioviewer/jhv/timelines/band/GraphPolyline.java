package org.helioviewer.jhv.timelines.band;

class GraphPolyline {

    private final IntArray xPoints;
    private final IntArray yPoints;

    GraphPolyline(IntArray _xPoints, IntArray _yPoints) {
        xPoints = _xPoints;
        yPoints = _yPoints;
    }

    int[] xPoints() {
        return xPoints.array();
    }

    int[] yPoints() {
        return yPoints.array();
    }

    int length() {
        return xPoints.length();
    }

}
