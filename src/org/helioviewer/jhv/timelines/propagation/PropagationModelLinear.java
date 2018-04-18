package org.helioviewer.jhv.timelines.propagation;

public class PropagationModelLinear implements PropagationModel {

    private final long timeshift;

    public PropagationModelLinear(long _timeshift) {
        timeshift = _timeshift;
    }

    @Override
    public long getDepropagatedTime(long ts) {
        return ts + timeshift;
    }

    @Override
    public long getPropagatedTime(long ts) {
        return ts - timeshift;
    }

}
