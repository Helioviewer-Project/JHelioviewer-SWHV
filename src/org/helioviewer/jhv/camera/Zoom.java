package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.layers.MovieDisplay;

class Zoom {

    private static final int FRAMES_PER_SECOND = 500;
    private static final int MILLIS_PER_FRAME = 1000 / FRAMES_PER_SECOND;

    private static final double SPEED_TOLERANCE = 0.0005;
    private static final double SPEED_LIMIT = 25;
    private static final double ACCELERATION_LIMIT = 2;
    private static final double VELOCITY_SMOOTHING = 0.80;

    private double velocity = 0;
    private double lastWheelDelta = 0;

    void zoom(Camera camera, double wheelDelta) {
        if (wheelDelta == 0) {
            return;
        }
        if (applyWheel(wheelDelta)) return;

        camera.zoom(velocity);
        if (velocity < 0)
            MovieDisplay.render(1);
        else
            MovieDisplay.display();
    }

    // Returns true when velocity is reset and no zoom should be applied.
    private boolean applyWheel(double wheel) {
        // Strong direction change in wheel stream: reset integration state.
        if (lastWheelDelta != 0 && lastWheelDelta * wheel < 0) {
            velocity = 0;
        }
        lastWheelDelta = wheel;

        // Integrate wheel impulse and smooth towards the target velocity.
        double lastVelocity = velocity;
        double targetVelocity = lastVelocity + wheel / MILLIS_PER_FRAME;
        velocity = lastVelocity + VELOCITY_SMOOTHING * (targetVelocity - lastVelocity);

        // Clamp acceleration to avoid sudden jumps.
        double acceleration = (velocity - lastVelocity) / MILLIS_PER_FRAME;
        if (Math.abs(acceleration) > ACCELERATION_LIMIT) {
            velocity = lastVelocity + ACCELERATION_LIMIT * MILLIS_PER_FRAME * Math.signum(acceleration);
        }

        // Clamp speed and snap tiny values to rest.
        double absVelocity = Math.abs(velocity);
        if (absVelocity > SPEED_LIMIT) {
            velocity = SPEED_LIMIT * Math.signum(velocity);
            return false;
        }
        if (absVelocity < SPEED_TOLERANCE) {
            velocity = 0;
            return true;
        }
        return false;
    }

}
