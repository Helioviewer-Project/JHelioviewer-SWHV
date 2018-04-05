package org.helioviewer.jhv.timelines.propagation;

public class PropagationModelLinear implements PropagationModel {

    long timeshift;

    public PropagationModelLinear(long _timeshift) {
        timeshift = _timeshift;
    }

    public long getDepropagatedTime(long ts) {
        return ts + timeshift;
    }

    public long getPropagatedTime(long ts) {
        return ts - timeshift;
    }
}
