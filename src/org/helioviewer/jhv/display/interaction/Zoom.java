package org.helioviewer.jhv.display.interaction;

import org.helioviewer.jhv.display.Camera;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.display.Viewport;

final class Zoom {

    private static final double SPEED_TOLERANCE = 0.0005;
    private static final double SPEED_LIMIT = 25;
    private static final double WHEEL_SENSITIVITY = 0.4;
    private static final double MAX_IMPULSE = 4;
    private static final long GESTURE_TIMEOUT_MS = 1000;

    private double velocity = 0;
    private double lastWheelDelta = 0;
    private long lastWheelTime = 0;

    void zoom(Viewport vp, double wheelDelta) {
        if (wheelDelta == 0) {
            return;
        }
        if (applyWheel(wheelDelta)) return;

        double factor = Camera.zoomFactor(velocity);
        if (Display.separateViewportZoom) {
            vp.zoom *= factor;
        } else {
            for (Viewport viewport : Display.getViewports())
                viewport.zoom *= factor;
        }
        if (velocity < 0)
            DisplayController.render(1);
        else
            DisplayController.display();
    }

    // Returns true when velocity is reset and no zoom should be applied.
    private boolean applyWheel(double wheel) {
        long now = System.currentTimeMillis();
        boolean timedOut = lastWheelTime != 0 && now - lastWheelTime > GESTURE_TIMEOUT_MS;
        lastWheelTime = now;

        // New gesture or direction change in wheel stream: reset integration state.
        if (timedOut || (lastWheelDelta != 0 && lastWheelDelta * wheel < 0)) {
            velocity = 0;
        }
        lastWheelDelta = wheel;

        // Limit each wheel impulse to avoid sudden jumps.
        double impulse = Math.clamp(wheel * WHEEL_SENSITIVITY, -MAX_IMPULSE, MAX_IMPULSE);
        velocity += impulse;

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
