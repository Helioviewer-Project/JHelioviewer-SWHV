package ch.fhnw.jhv.gui.controller.cam;

import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;

import javax.media.opengl.GL;

/**
 * Defines all Method for a Camera to set all Projection settings.
 * 
 * @author David Hostettler (davidhostettler@gmail.com)
 * @date 14.06.2011
 * 
 */
public interface Camera extends MouseListener, MouseWheelListener, MouseMotionListener, KeyListener {

    /**
     * Sets the MODEL_VIEW rotation matrix in opengl for this camera
     * 
     * @param gl
     */
    public void setView(GL gl);

    /**
     * Sets the projection of this camera
     * 
     * @param gl
     *            OpenGL Context
     * @param width
     *            Width of Screen
     * @param height
     *            Height of Screen
     */
    public void setProjection(GL gl, int width, int height);

    /**
     * Set the parameter for the OpenGL projection plane
     * 
     * @param near
     *            Distance to near projection plane
     * @param far
     *            Distance to far projection plane
     */
    public void setProjectionPlane(float near, float far);

    /**
     * Set the mouse sensitivity of the camera
     * 
     * @param newSense
     */
    public void setSensitivity(float newSense);

    /**
     * Get the current Mouse sensitivity
     * 
     * @return
     */
    public float getSensitivity();

    /**
     * Perform all the operation to remove the camera
     */
    public void dettach();

    /**
     * Get the name of the camera
     * 
     * @return a description to use in UI Elements
     */
    public String getLabel();
}
