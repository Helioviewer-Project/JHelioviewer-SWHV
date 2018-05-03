package org.helioviewer.jhv.timelines.propagation;

import org.helioviewer.jhv.astronomy.Sun;

public class PropagationModelRadial implements PropagationModel {

    private final boolean isPropagated;
    private final double radiusMilli;

    public PropagationModelRadial(double speed) {
        isPropagated = speed > 0;
        radiusMilli = isPropagated ? Sun.RadiusMeter / speed : 0; // km/s = m/msec
    }

    @Override
    public boolean isPropagated() {
        return isPropagated;
    }

    @Override
    public long getInsituTime(long ts) {
        return isPropagated ? ts + (long) (getInsituDistance(ts) * radiusMilli + .5) : ts;
    }

    @Override
    public long getSunTime(long ts) {
        return isPropagated ? ts - (long) (getInsituDistance(ts) * radiusMilli + .5) : ts;
    }

    private static double getInsituDistance(long ts) {
        return Sun.getEarthDistance(ts) * Sun.L1Factor;
    }

}
