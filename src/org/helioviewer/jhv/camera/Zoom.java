package org.helioviewer.jhv.camera;

import java.util.ArrayList;

import org.helioviewer.jhv.layers.MovieDisplay;

// from https://github.com/opus1269/smooth-scroller
class Zoom {

    private static final int FRAMES_PER_SECOND = 500;
    private static final int MILLIS_PER_FRAME = 1000 / FRAMES_PER_SECOND;

    private double mVelocity = 0;
    private double mLastWheelDelta = 0;

    private final ArrayList<Double> mVelocities = new ArrayList<>();
    private static final int MAX_VELOCITIES = 10;

    private static final double spdTol = 0.0005;
    private static final double spdLmt = 25;
    private static final double accLmt = 5;
    // private static final double lambda = 0.005;

    void zoom(Camera camera, double wheelDelta) {
        if (wheelDelta == 0)
            return;

        boolean sameDirection = mLastWheelDelta * wheelDelta > 0.0;
        mLastWheelDelta = wheelDelta;

        if (!sameDirection) {
            // changed direction
            zeroVelocity();
            return;
        }

        // calculate new velocity increment
        double deltaV = wheelDelta / MILLIS_PER_FRAME;
        double oldVelocity = mVelocity;
        double newVelocity = mVelocity + deltaV;

        // calculate average velocity over last several mouse wheel events
        if (mVelocities.size() == MAX_VELOCITIES) {
            mVelocities.remove(0);
        }
        mVelocities.add(newVelocity);

        mVelocity = getAverage(mVelocities);
        // limit acceleration
        double acc = (mVelocity - oldVelocity) / MILLIS_PER_FRAME;
        if (Math.abs(acc) > accLmt) {
            mVelocity = oldVelocity + accLmt * MILLIS_PER_FRAME * Math.signum(acc);
        }
        // mVelocity *= Math.exp(-lambda * MILLIS_PER_FRAME);

        // limit speed
        if (Math.abs(mVelocity) > spdLmt) {
            mVelocity = spdLmt * Math.signum(mVelocity);
        }
        if (Math.abs(mVelocity) < spdTol) {
            zeroVelocity();
        }

        if (mVelocity != 0) {
            camera.zoom(mVelocity);
            if (mVelocity > 0) {
                MovieDisplay.render(1);
            } else
                MovieDisplay.display();
        }
    }

    private void zeroVelocity() {
        mVelocity = 0;
        mVelocities.clear();
    }

    private double getAverage(ArrayList<Double> array) {
        double sum = 0;
        if (!array.isEmpty()) {
            for (double item : array) {
                sum = sum + item;
            }
            return sum / array.size();
        }
        return sum;
    }

}
