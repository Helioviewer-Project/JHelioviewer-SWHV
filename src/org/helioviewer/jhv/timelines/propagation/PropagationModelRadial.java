package org.helioviewer.jhv.timelines.propagation;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.time.JHVDate;

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

    private double getInsituDistance(long ts) {
        return Sun.getEarth(new JHVDate(ts)).rad * Sun.L1Factor;
    }

}
