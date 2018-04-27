package org.helioviewer.jhv.timelines.propagation;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.time.JHVDate;

public class PropagationModelRadial implements PropagationModel {

    private final boolean isPropagated;
    private final double radiusMilli;

    public PropagationModelRadial(double speed) {
        isPropagated = !(speed <= 0);
        radiusMilli = Sun.RadiusMeter / speed; // km/s = m/msec
    }

    @Override
    public boolean isPropagated() {
        return isPropagated;
    }

    @Override
    public long getDepropagatedTime(long ts) {
        return isPropagated ? depropagateTime(ts) : ts;
    }

    @Override
    public long getPropagatedTime(long ts) {
        return isPropagated ? propagateTime(ts) : ts;
    }

    private long depropagateTime(long ts) {
        long sunTime = ts - Display.getCamera().getViewpoint().lightTime;
        return sunTime + (long) (getInsituDistance(ts) * radiusMilli + .5);
    }

    private long propagateTime(long ts) {
        long sunTime = ts - (long) (getInsituDistance(ts) * radiusMilli + .5);
        return sunTime + Display.getCamera().getViewpoint().lightTime;
    }

    private double getInsituDistance(long ts) {
        return Sun.getEarth(new JHVDate(ts)).rad * Sun.L1Factor;
    }

}
