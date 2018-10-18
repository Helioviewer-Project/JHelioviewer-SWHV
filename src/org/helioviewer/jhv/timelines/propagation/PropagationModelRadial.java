package org.helioviewer.jhv.timelines.propagation;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.math.MathUtils;

public class PropagationModelRadial implements PropagationModel {

    private final boolean isPropagated;
    private final double radiusMilli;

    public PropagationModelRadial(double _speed) { // km/s
        double speed = MathUtils.clip(_speed * 1e3, 0, Sun.CLIGHT); // m/s
        isPropagated = speed > 0;
        radiusMilli = isPropagated ? Sun.RadiusMeter / speed * 1e3 : 0;
    }

    @Override
    public boolean isPropagated() {
        return isPropagated;
    }

    @Override
    public long getObservationTime(long ts) {
        return isPropagated ? (long) (ts + radiusMilli * getObservationDistance(ts) - getRSShift() + .5) : ts;
    }

    @Override
    public long getViewpointTime(long ts) {
        return isPropagated ? (long) (ts - radiusMilli * getObservationDistance(ts) + getRSShift() + .5) : ts;
    }

    private static double getObservationDistance(long ts) {
        return Sun.getEarthDistance(ts) * Sun.L1Factor;
    }

    private static double getRSShift() {
        return Sun.RadiusMilli * Display.getCamera().getViewpoint().distance;
    }

}
