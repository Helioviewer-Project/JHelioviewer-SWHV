package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.layers.MovieDisplay;

class Zoom {

    private static final int FRAMES_PER_SECOND = 500;
    private static final int MILLIS_PER_FRAME = 1000 / FRAMES_PER_SECOND;

    private static final double SPEED_TOLERANCE = 0.0005;
    private static final double SPEED_LIMIT = 25;
    private static final double ACCELERATION_LIMIT = 8;
    private static final double VELOCITY_SMOOTHING = 0.66;
    private static final double WHEEL_NOISE_TOLERANCE = 0.08;
    private static final double REVERSE_WHEEL_TOLERANCE = 0.18;
    private static final double ZERO_CROSS_VELOCITY_TOLERANCE = 0.12;

    private double velocity = 0;
    private double lastWheelDelta = 0;

    void zoom(Camera camera, double wheelDelta) {
        if (wheelDelta == 0) {
            return;
        }

        double wheel = wheelDelta;
        // Reversal guard: brake or reset first so we do not "coast" in the old direction.
        if (velocity != 0 && wheel * velocity < 0) {
            if (Math.abs(wheel) < REVERSE_WHEEL_TOLERANCE) {
                velocity *= 0.2;
                if (Math.abs(velocity) < SPEED_TOLERANCE) {
                    velocity = 0;
                }
                lastWheelDelta = 0;
                return;
            }
            velocity = 0;
        }

        double absWheel = Math.abs(wheel);
        // Input denoising: drop tiny deltas and weak opposite-sign spikes from touchpads.
        if (absWheel < WHEEL_NOISE_TOLERANCE ||
                (lastWheelDelta != 0 && wheel * lastWheelDelta < 0 && absWheel < REVERSE_WHEEL_TOLERANCE)) {
            wheel = 0;
        }

        // No wheel impulse: apply decay so zoom naturally comes to rest.
        if (wheel == 0) {
            velocity *= 0.5;
            if (Math.abs(velocity) < SPEED_TOLERANCE) {
                velocity = 0;
                lastWheelDelta = 0;
                return;
            }
        } else if (applyWheel(wheel)) return;

        if (velocity == 0) return;

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

        // Clamp speed and snap tiny near-zero sign flips to rest.
        double absVelocity = Math.abs(velocity);
        if (absVelocity > SPEED_LIMIT) {
            velocity = SPEED_LIMIT * Math.signum(velocity);
            return false;
        }
        if ((Math.signum(velocity) != Math.signum(lastVelocity) && absVelocity < ZERO_CROSS_VELOCITY_TOLERANCE) || absVelocity < SPEED_TOLERANCE) {
            velocity = 0;
            return true;
        }
        return false;
    }

}
