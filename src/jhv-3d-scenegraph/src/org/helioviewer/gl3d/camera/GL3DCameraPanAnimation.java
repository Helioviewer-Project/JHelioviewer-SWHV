package org.helioviewer.gl3d.camera;

import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;

/**
 * This animations changes the camera's panning (x- and y-translation) by the
 * specified amount.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DCameraPanAnimation implements GL3DCameraAnimation {
    private boolean isFinished = false;

    private long startTime = -1;
    private long lastAnimationTime = -1;
    private long timeLeft = 0;

    private GL3DVec3d distanceToMove;
    private GL3DVec3d distanceDelta;
    private GL3DVec3d targetTranslation;

    public GL3DCameraPanAnimation(GL3DVec3d distanceToMove) {
        this(distanceToMove, GL3DCameraAnimation.DEFAULT_ANIMATION_TIME);
    }

    public GL3DCameraPanAnimation(GL3DVec3d distanceToMove, long duration) {
        this.distanceToMove = distanceToMove;
        this.timeLeft = duration;
        this.distanceDelta = GL3DVec3d.divide(distanceToMove, this.timeLeft);
    }

    public void animate(GL3DCamera camera) {
        if (this.startTime < 0) {
            this.startTime = System.currentTimeMillis();
            this.lastAnimationTime = System.currentTimeMillis();
            this.targetTranslation = camera.getTranslation().copy().add(distanceToMove);
        }

        long timeDelta = System.currentTimeMillis() - lastAnimationTime;

        this.timeLeft -= timeDelta;

        if (timeLeft <= 0) {
            camera.setPanning(this.targetTranslation.x, this.targetTranslation.y);
            this.isFinished = true;
            camera.updateCameraTransformation(true);
        } else {
            GL3DVec3d translation = GL3DVec3d.multiply(this.distanceDelta, timeDelta);
            camera.addPanning(translation.x, translation.y);
            camera.updateCameraTransformation(false);
        }

        this.lastAnimationTime = System.currentTimeMillis();
    }

    public void updateWithAnimation(GL3DCameraAnimation animation) {
        if (animation instanceof GL3DCameraPanAnimation) {
            GL3DCameraPanAnimation ani = (GL3DCameraPanAnimation) animation;
            this.timeLeft = Math.min(2000, this.timeLeft / 5 + ani.timeLeft);
            this.distanceToMove.add(ani.distanceToMove);
            this.distanceDelta = GL3DVec3d.divide(distanceToMove, this.timeLeft);
        }
    }

    public boolean isFinished() {
        return isFinished;
    }
}
