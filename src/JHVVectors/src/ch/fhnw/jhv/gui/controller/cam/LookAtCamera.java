package ch.fhnw.jhv.gui.controller.cam;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.media.opengl.GL;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import ch.fhnw.jhv.gui.viewport.components.SunRenderPlugin;
import ch.fhnw.jhv.plugins.PluginManager;
import ch.fhnw.jhv.plugins.interfaces.RenderPlugin.RenderPluginType;

/**
 * The RotationCamera is zoomable Camera that lets the user look at the sun as
 * he's used to in other applications like Google Earth etc. This Camera only
 * rotates the view around 2-Axis and the sun cannot be turned upside down.
 * (unlike with TrackBallCamera which rotates around 3-Axis)
 * 
 * To make the ease-out movement of the sun we average the last few mouse
 * movement vector to calculate the direction vector. The direction vector is
 * then applied to the rotation every frame and decreased with a friction
 * factor.
 * 
 * @author David Hostettler (davidhostettler@gmail.com)
 * @date 04.07.2011
 * 
 */
public class LookAtCamera extends AbstractZoomCamera {

    /**
     * the rotation of the camera around the X-Axis (or the cameras inclination)
     */
    protected float inclination;

    /**
     * the rotation of the camera around the Y-Axis (or the cameras azimuth)
     */
    protected float azimuth;

    /**
     * the X-Position of the mouse at the last drag event (or where the drag
     * started)
     */
    private int oldMouseX = 0;

    /**
     * the X-Position of the mouse at the last drag event (or where the drag
     * started)
     */
    private int oldMouseY = 0;

    /**
     * Array to store previous "mouse movement vectors" of the mouse dragged
     * event. Several movement vectors are used for averaging the direction
     * vector of the cameras rotation. This is necessary because the
     * mouseDragged event fires at short intervals, so one single mouse movement
     * is inaccurate for the ease-out camera rotation when the mouse is released
     * after dragging. This array is accessed ring-buffer like.
     */
    int previousMousePos[][];

    /**
     * The count of "mouse movement vectors" to be used for averaging the
     * direction vector of the cameras rotation
     */
    int vectorCount = 10;

    /**
     * The counter of the mouse movement vectors currently stored
     */
    int currentPosCount;

    /**
     * the first index of the previousMousePos-Array to be used for averaging
     * the direction vector
     */
    int startIndex;

    /**
     * The friction that is applied to the cameras rotation after each frame.
     * This parameter is currently frame-rate dependent.For this reason
     * increasing the framerate has the side effect that the suns turns slower.
     * (The friction parameter is then more often applied).
     */
    float friction = 0.9f;

    /**
     * if set to true, the camera adjusts the center
     */
    boolean isMovingCenter = false;

    /**
     * instance to the plugin that renders the center point of the camera
     */
    CameraCenterPointRenderPlugin centerRenderer;

    /**
     * The timestamp when the last MouseDragged event occurred
     */
    long lastMouseDrag;

    /**
     * the averaged mouse movement vector
     */
    Vector2f direction = new Vector2f();

    /**
     * Eye-Vector is the position of the camera
     */
    Vector3f eye = new Vector3f();

    /**
     * Lookat-Vector is the point the camera looks at
     */
    Vector3f lookat = new Vector3f(0, 0, 0);

    protected boolean useNormalZoomFactor;

    /**
     * Constructor
     */
    public LookAtCamera() {
        super();

        // init the empty array to store the mouse movement vectors
        previousMousePos = new int[vectorCount][2];
        startIndex = currentPosCount = 0;
        direction.x = direction.y = 0;

        this.enableVariableZoomDelta(true);
        this.enableZoomLimit(true);

        inclination = 0;
        azimuth = 0;

        zoom = 55;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ch.fhnw.jhv.gui.controller.cam.AbstractCamera#setView(javax.media.opengl
     * .GL)
     */

    public void setView(GL gl) {
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glLoadIdentity();

        // distance from camera to sun surface
        azimuth += direction.x * mouseSense * getRotationFactor();
        inclination += direction.y * mouseSense * getRotationFactor();

        // check if direction is 0
        if (direction.length() < 0.01) {
            // we have to make sure to reset the average vector!
            currentPosCount = startIndex = 0;
        }
        // TODO currently the friction factor is dependent on the framerate.
        // this means that if the framerate increases the sun will turn slower
        // this must be fixed when the framerate can be adjusted by the user
        direction.scale(friction);

        clampRotation();

        float azze = -(float) Math.toRadians(azimuth);
        float incli = (float) Math.toRadians(inclination);

        eye.set((float) (Math.sin(azze) * Math.cos(incli)), (float) (Math.sin(incli)), (float) (Math.cos(azze) * Math.cos(incli)));
        eye.scale(zoom);
        eye.add(lookat, eye);

        glu.gluLookAt(eye.x, eye.y, eye.z, lookat.x, lookat.y, lookat.z, 0, 1, 0);

    }

    /**
     * Get the Rotation Friction Factor depending on the current zoom
     * 
     * @return
     */
    protected float getRotationFactor() {
        if (useNormalZoomFactor)
            return normalZoom;
        // distance from camera to sun surface
        float distToSun = zoom - SunRenderPlugin.SUN_RADIUS;
        // the factor that adjusts the rotation speed of the sun according to
        // the zoom
        return (distToSun > normalZoom ? 1 : distToSun / normalZoom);
    }

    /**
     * Clamp the Rotations
     */
    protected void clampRotation() {
        if (inclination > 80) {
            inclination = 80;
        } else if (inclination < -80) {
            inclination = -80;
        }

        if (azimuth < -360) {
            azimuth += 360;
        } else if (azimuth > 360) {
            azimuth -= 360;
        }
    }

    /**
     * Calculates and sets the average of the previously stored mouse movement
     * vectors into the direction variable
     */
    private void calculateDirection() {
        int index = startIndex, sumx = 0, sumy = 0;
        int counter = 0;

        for (int i = 0; i < currentPosCount; i++) {
            sumx += previousMousePos[index][0];
            sumy += previousMousePos[index][1];
            index = (index + 1) % (vectorCount);
            counter++;
        }

        direction.x = (float) sumx / currentPosCount;
        direction.y = (float) sumy / currentPosCount;
        direction.scale((float) counter / (float) vectorCount);
    }

    /**
     * initializes variables for recording of mouse movement vectors
     */

    public void mousePressed(MouseEvent e) {
        oldMouseX = e.getX();
        oldMouseY = e.getY();

        currentPosCount = startIndex = 0;
        direction.x = direction.y = 0;
    }

    /**
     * Records the mouse movement vectors into the previousMousePos.
     * 
     */

    public void mouseDragged(MouseEvent e) {
        if (centerRenderer == null) {
            long time = System.currentTimeMillis();

            // TRY TO FIX BUGS WHEN THE USERS KEEPS DRAGGING AND CHANGES
            // ROTATION DIRECTION
            // DOESNT WORK WELL YET
            if (time - lastMouseDrag > 200) {
                // if the last drag event is too long ago we clear
                // the mouse vectors for averaging direction of rotation
                currentPosCount = startIndex = 0;
            }

            lastMouseDrag = time;

            // difference to last mouse position
            int diffx = e.getX() - oldMouseX;
            int diffy = e.getY() - oldMouseY;

            // calculate next index in currentPosCount
            int currentIndex = (startIndex + currentPosCount) % vectorCount;

            previousMousePos[currentIndex][0] = diffx;
            previousMousePos[currentIndex][1] = diffy;

            oldMouseX = e.getX();
            oldMouseY = e.getY();

            if (currentPosCount < vectorCount) {
                // increase the vector count
                currentPosCount += 1;
            } else {
                // the array of vectors is allready full, so we increase the
                // startindex.
                startIndex = (startIndex + 1) % vectorCount;
            }
            calculateDirection();
        } else {
            Vector3f dir = new Vector3f(e.getX() - oldMouseX, 0, e.getY() - oldMouseY);
            oldMouseX = e.getX();
            oldMouseY = e.getY();
            dir.scale(0.1f);
            centerRenderer.move(dir, -azimuth);
            lookat = centerRenderer.getPosition();
        }

    }

    public void dettach() {
        if (centerRenderer != null) {
            PluginManager.getInstance().deactivateRenderPluginType(RenderPluginType.CAMERACENTERPOINT);
            centerRenderer = null;
        }
    }

    /**
     * Creates a CameraCenterPointRenderPlugin on double-click and adds it to
     * the plugin-manager. If there is already a CenterPoint it deactivates it.
     */

    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
            if (centerRenderer == null) {
                int min = -(int) (2f * SunRenderPlugin.SUN_RADIUS);
                int max = (int) (2f * SunRenderPlugin.SUN_RADIUS);

                centerRenderer = new CameraCenterPointRenderPlugin(lookat, max, min);

                PluginManager.getInstance().updateRenderPlugingReference(RenderPluginType.CAMERACENTERPOINT, centerRenderer);
                PluginManager.getInstance().activateRenderPluginType(RenderPluginType.CAMERACENTERPOINT);

                oldMouseX = e.getX();
                oldMouseY = e.getY();
            } else {
                PluginManager.getInstance().deactivateRenderPluginType(RenderPluginType.CAMERACENTERPOINT);
                lookat = centerRenderer.getPosition();
                centerRenderer = null;
            }
        }
    }

    /**
     * Adjusts the zoom of the camera. If the camera gets near the sun the zoom
     * is adjusted in smaller steps.
     */

    public void mouseWheelMoved(MouseWheelEvent e) {
        if (centerRenderer == null) {
            super.mouseWheelMoved(e);
        } else {
            int distance = -e.getWheelRotation();
            Vector3f dir = new Vector3f(0, distance, 0);
            centerRenderer.move(dir, 0);
            lookat = centerRenderer.getPosition();
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.gui.controller.cam.Camera#getLabel()
     */
    public String getLabel() {
        return "Sun Rotation Camera";
    }

}
