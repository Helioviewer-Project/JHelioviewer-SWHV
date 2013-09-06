package org.helioviewer.gl3d.camera;

/**
 * GL3DCameraAnimations are used to continuously change the camera over a
 * certain amount of time. Register animations to the currently active
 * {@link GL3DCamera}. Make sure that isFinished returns true as soon as the
 * animation should stop.
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 * 
 */
public interface GL3DCameraAnimation {
    public static final long DEFAULT_ANIMATION_TIME = 600;

    /**
     * Animate pass, called once per render loop by the {@link GL3DCamera}.
     * 
     * @param camera
     *            Active camera that this animation can be applied to.
     */
    public void animate(GL3DCamera camera);

    /**
     * Return true if the animation has finished. The animation will then be
     * removed from the camera.
     * 
     * @return true if the animation has already finished
     */
    public boolean isFinished();

    /**
     * If an animation needs to be changed while it is running, this method will
     * be called. The new animation is given as a parameter.
     * 
     * @param animation
     *            Animation that needs to be incorporated
     */
    public void updateWithAnimation(GL3DCameraAnimation animation);
}
