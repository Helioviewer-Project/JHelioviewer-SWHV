package org.helioviewer.gl3d.camera;

import org.helioviewer.jhv.display.Displayer;

/**
 * This animation zooms the camera by a given amount. Zooming only affects the
 * z-component of the {@link GL3DCamera}'s translation.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DCameraZoomAnimation implements GL3DCameraAnimation {
    private boolean isFinished = false;

    private long startTime = -1;
    private long lastAnimationTime = -1;
    private long timeLeft = 0;

    private double fovToTravel;
    private double fovDelta;

    private double targetFov;

    public GL3DCameraZoomAnimation(double distanceToTravel) {
        this(distanceToTravel, GL3DCameraAnimation.DEFAULT_ANIMATION_TIME);
        Displayer.getSingletonInstance().render();
    }

    public GL3DCameraZoomAnimation(double fovToTravel, long duration) {
        this.fovToTravel = fovToTravel;
        this.timeLeft = duration;
        this.fovDelta = fovToTravel / this.timeLeft;
    }

    @Override
    public void animate(GL3DCamera camera) {
        if (this.startTime < 0) {
            this.startTime = System.currentTimeMillis();
            this.lastAnimationTime = System.currentTimeMillis();
            this.targetFov = Math.min(GL3DCamera.MAX_FOV, Math.max(GL3DCamera.MIN_FOV, camera.getCameraFOV() + this.fovToTravel));
        }

        long timeDelta = System.currentTimeMillis() - lastAnimationTime;

        this.timeLeft -= timeDelta;

        if (timeLeft <= 0) {
            camera.setCameraFOV(this.targetFov);
        } else {
            double fovTranslation = Math.min(camera.getCameraFOV() + this.fovDelta * timeDelta, targetFov);
            if (this.fovToTravel < 0) {
                fovTranslation = Math.max(camera.getCameraFOV() + this.fovDelta * timeDelta, targetFov);
            }
            camera.setCameraFOV(fovTranslation);
        }

        if (camera.getCameraFOV() == this.targetFov) {
            this.isFinished = true;
            camera.updateCameraTransformation(true);
            Displayer.getSingletonInstance().render();
        } else {
            camera.updateCameraTransformation(true);
            Displayer.getSingletonInstance().display();
        }

        this.lastAnimationTime = System.currentTimeMillis();

    }

    @Override
    public void updateWithAnimation(GL3DCameraAnimation animation) {
        if (animation instanceof GL3DCameraZoomAnimation) {
            GL3DCameraZoomAnimation ani = (GL3DCameraZoomAnimation) animation;
            this.timeLeft = Math.min(2000, this.timeLeft / 5 + ani.timeLeft);
            this.fovToTravel += ani.fovToTravel;
            this.targetFov = Math.min(GL3DCamera.MAX_FOV, Math.max(GL3DCamera.MIN_FOV, this.targetFov + ani.fovToTravel));
            this.fovDelta = this.fovToTravel / this.timeLeft;
        }
    }

    @Override
    public boolean isFinished() {
        return isFinished;
    }
}
