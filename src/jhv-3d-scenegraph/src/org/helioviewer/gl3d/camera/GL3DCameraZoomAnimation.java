package org.helioviewer.gl3d.camera;

/**
 * This animation zooms the camera by a given amount. Zooming only affects the
 * z-component of the {@link GL3DCamera}'s translation.
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DCameraZoomAnimation implements GL3DCameraAnimation {
    private boolean isFinished = false;

    private long startTime = -1;
    private long lastAnimationTime = -1;
    private long timeLeft = 0;

    private double distanceToTravel;
    private double distanceDelta;

    private double targetDistance;

    public GL3DCameraZoomAnimation(double distanceToTravel) {
        this(distanceToTravel, GL3DCameraAnimation.DEFAULT_ANIMATION_TIME);
    }

    public GL3DCameraZoomAnimation(double distanceToTravel, long duration) {
        this.distanceToTravel = distanceToTravel;
        this.timeLeft = duration;
        this.distanceDelta = distanceToTravel / this.timeLeft;
    }

    public void animate(GL3DCamera camera) {
        if (this.startTime < 0) {
            this.startTime = System.currentTimeMillis();
            this.lastAnimationTime = System.currentTimeMillis();
            this.targetDistance = Math.min(GL3DCamera.MIN_DISTANCE, Math.max(GL3DCamera.MAX_DISTANCE, camera.getZTranslation() + this.distanceToTravel));
        }

        long timeDelta = System.currentTimeMillis() - lastAnimationTime;

        this.timeLeft -= timeDelta;

        if (timeLeft <= 0) {
            camera.setZTranslation(targetDistance);
        } else {
            double zTranslation = Math.min(camera.getZTranslation() + this.distanceDelta * timeDelta, targetDistance);
            if (this.distanceToTravel < 0) {
                zTranslation = Math.max(camera.getZTranslation() + this.distanceDelta * timeDelta, targetDistance);
            }
            camera.setZTranslation(zTranslation);
        }

        if (camera.getZTranslation() == this.targetDistance) {
            this.isFinished = true;
            camera.updateCameraTransformation(true);
        } else {
            camera.updateCameraTransformation(false);
        }

        this.lastAnimationTime = System.currentTimeMillis();
    }

    public void updateWithAnimation(GL3DCameraAnimation animation) {
        if (animation instanceof GL3DCameraZoomAnimation) {
            GL3DCameraZoomAnimation ani = (GL3DCameraZoomAnimation) animation;
            this.timeLeft = Math.min(2000, this.timeLeft / 5 + ani.timeLeft);
            this.distanceToTravel += ani.distanceToTravel;
            this.targetDistance = Math.min(GL3DCamera.MIN_DISTANCE, Math.max(GL3DCamera.MAX_DISTANCE, this.targetDistance + ani.distanceToTravel));
            this.distanceDelta = this.distanceToTravel / this.timeLeft;
        }
    }

    public boolean isFinished() {
        return isFinished;
    }
}
