package org.helioviewer.gl3d.camera;

import org.helioviewer.gl3d.scenegraph.math.GL3DQuatd;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;

/**
 * This animation rotates the camera from a startpoint to an endpoint by using
 * the {@link GL3DQuatd}'s slerp interpolation.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DCameraRotationAnimation implements GL3DCameraAnimation {
    private boolean isFinished = false;

    private long startTime = -1;
    private long lastAnimationTime = -1;
    private long timeLeft = 0;
    private long duration = 0;

    private GL3DVec3d startPoint;
    private GL3DVec3d endPoint;
    private GL3DQuatd targetRotation;

    public GL3DCameraRotationAnimation(GL3DVec3d startPoint, GL3DVec3d endPoint) {
        this(startPoint, endPoint, GL3DCameraAnimation.DEFAULT_ANIMATION_TIME);
    }

    public GL3DCameraRotationAnimation(GL3DVec3d startPoint, GL3DVec3d endPoint, long duration) {
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.timeLeft = duration;
        this.duration = duration;
    }

    public void animate(GL3DCamera camera) {
        if (this.startTime < 0) {
            this.startTime = System.currentTimeMillis();
            this.lastAnimationTime = System.currentTimeMillis();
            // Create rotation Quaternion
            GL3DQuatd rotation = GL3DQuatd.calcRotation(this.endPoint, this.startPoint);
            // Copy the current rotation to not loose that information during
            // the animation
            this.targetRotation = camera.getRotation().copy();
            this.targetRotation.rotate(rotation);
        }

        long timeDelta = System.currentTimeMillis() - lastAnimationTime;

        this.timeLeft -= timeDelta;

        if (timeLeft <= 0) {
            camera.getRotation().set(this.targetRotation);
            this.isFinished = true;
        } else {
            double t = 1 - ((double) this.timeLeft) / this.duration;
            // Apply rotation interpolation
            camera.getRotation().set(camera.getRotation().nlerp(this.targetRotation, t));
        }

        camera.updateCameraTransformation();

        this.lastAnimationTime = System.currentTimeMillis();
    }

    public void updateWithAnimation(GL3DCameraAnimation animation) {
        // TODO: Simon Sp�rri: implement Rotation Animation update
        // throw new UnsupportedOperationException();
        // if(animation instanceof GL3DCameraRotationAnimation) {
        // GL3DCameraRotationAnimation ani =
        // (GL3DCameraRotationAnimation)animation;
        // this.timeLeft = Math.min(2000, this.timeLeft/5+ani.timeLeft);
        // GL3DQuatd rotation = GL3DQuatd.calcRotation(this.endPoint,
        // this.startPoint);
        // this.targetRotation = .getRotation().copy().rotate(rotation);
        // this.targetRotation.rotate(rotation);
        // }
    }

    public boolean isFinished() {
        return isFinished;
    }
}
