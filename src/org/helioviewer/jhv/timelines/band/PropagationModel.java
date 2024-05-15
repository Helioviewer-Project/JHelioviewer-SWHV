package org.helioviewer.jhv.timelines.band;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.time.TimeUtils;

interface PropagationModel {

    boolean isPropagated();

    long getObservationTime(long ts);

    long getViewpointTime(long ts);

    class Delay implements PropagationModel {

        private final boolean isPropagated;
        private final double delayMilli;

        Delay(double _delay) { // days
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

    class Radial implements PropagationModel {

        private final boolean isPropagated;
        private final double radiusMilli;

        Radial(double _speed) { // km/s
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

}
