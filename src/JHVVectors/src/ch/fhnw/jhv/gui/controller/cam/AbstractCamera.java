package ch.fhnw.jhv.gui.controller.cam;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;

/**
 * This is the Base Class for all the Cameras. It implements a perspective
 * projection by default.
 * 
 * 
 * @author David Hostettler (davidhostettler@gmail.com)
 * @date 04.07.2011
 * 
 */
public abstract class AbstractCamera implements Camera {

    /**
     * height and width of screen. this information is needed to set the
     * projection
     */
    protected int screenWidth, screenHeight;

    /**
     * glu object
     */
    protected GLU glu = new GLU();

    /**
     * near and far plane
     */
    protected float near = 0.1f, far = 1000f;

    /**
     * sensitivity of the mouse
     */
    protected float mouseSense = 0.2f;

    /**
     * Initialize camera with distance of near and far pane
     * 
     * @param near
     *            Distance to near pane
     * @param far
     *            Distance to far pane
     */
    public AbstractCamera(float near, float far) {
        this.near = near;
        this.far = far;
    }

    public AbstractCamera() {

    }

    /**
     * This method sets the view of the camera. It is called before any object
     * is rendered.
     */
    public abstract void setView(GL gl);

    /**
     * Sets a Perspective Projection with a FOV of 45 degrees
     */
    public void setProjection(GL gl, int width, int height) {
        double yxRatio = (float) width / height;
        screenHeight = height;
        screenWidth = width;
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(45, yxRatio, near, far);
        gl.glViewport(0, 0, width, height);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.gui.controller.cam.Camera#setProjectionPlane(float,
     * float)
     */
    public void setProjectionPlane(float near, float far) {
        this.near = near;
        this.far = far;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.gui.controller.cam.Camera#dettach()
     */
    public void dettach() {
        // empty implementation because basic behaviour is doing nothing
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.gui.controller.cam.Camera#setSensitivity(float)
     */
    public void setSensitivity(float newSense) {
        this.mouseSense = newSense;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.gui.controller.cam.Camera#getSensitivity()
     */
    public float getSensitivity() {
        return this.mouseSense;
    }

    // definition of all the GUI related listeners
    // so they can be overridden in the implementation when necessary

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
    }

    public void mouseMoved(MouseEvent e) {
    }

    public void keyTyped(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
    }

}
