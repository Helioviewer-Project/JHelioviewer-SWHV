package org.helioviewer.jhv.timelines.propagation;

public class PropagationModelRadial implements PropagationModel {

    private final double speed;

    public PropagationModelRadial(double _speed) {
        speed = _speed;
    }

    @Override
    public long getDepropagatedTime(long ts) {
        return ts; //+ timeshift;
    }

    @Override
    public long getPropagatedTime(long ts) {
        return ts; //- timeshift;
    }

}
