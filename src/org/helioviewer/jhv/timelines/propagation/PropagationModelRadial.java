package org.helioviewer.jhv.timelines.propagation;

public class PropagationModelRadial implements PropagationModel {

    private final boolean isPropagated;
    private final double speed;

    public PropagationModelRadial(double _speed) {
        speed = _speed;
        isPropagated = !(speed <= 0);
    }

    @Override
    public boolean isPropagated() {
        return isPropagated;
    }

    @Override
    public long getDepropagatedTime(long ts) {
        return isPropagated ? ts + getTimeshift() : ts;
    }

    @Override
    public long getPropagatedTime(long ts) {
        return isPropagated ? ts - getTimeshift() : ts;
    }

    private long getTimeshift() {
        return 0;
    }

}
