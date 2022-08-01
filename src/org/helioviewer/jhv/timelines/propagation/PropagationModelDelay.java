package org.helioviewer.jhv.timelines.propagation;

import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.time.TimeUtils;

public class PropagationModelDelay implements PropagationModel {

    private final boolean isPropagated;
    private final double delayMilli;

    public PropagationModelDelay(double _delay) { // days
        delayMilli = MathUtils.clip(_delay * TimeUtils.DAY_IN_MILLIS, 0, 100 * TimeUtils.DAY_IN_MILLIS); // millis
        isPropagated = delayMilli > 0;
    }

    @Override
    public boolean isPropagated() {
        return isPropagated;
    }

    @Override
    public long getObservationTime(long ts) {
        return isPropagated ? (long) (ts + delayMilli) : ts;
    }

    @Override
    public long getViewpointTime(long ts) {
        return isPropagated ? (long) (ts - delayMilli) : ts;
    }

}
