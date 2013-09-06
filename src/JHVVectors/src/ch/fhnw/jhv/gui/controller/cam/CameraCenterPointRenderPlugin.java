package ch.fhnw.jhv.gui.controller.cam;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;

import ch.fhnw.jhv.helper.MathHelper;
import ch.fhnw.jhv.plugins.interfaces.AbstractPlugin;
import ch.fhnw.jhv.plugins.interfaces.RenderPlugin;

import com.sun.opengl.util.GLUT;

/**
 * Renders the movable Camera Center point. This Center Point is placed by a
 * camera that cand adjust the lookAT / Center.
 * 
 * @author David Hostettler (davidhostettler@gmail.com)
 * 
 *         18.08.2011
 */
public class CameraCenterPointRenderPlugin extends AbstractPlugin implements RenderPlugin {

    /**
     * Glu Object
     */
    GLU glu = new GLU();

    /**
     * Glut Object
     */
    GLUT glut = new GLUT();

    /**
     * Position of the center point
     */
    Vector3f position;

    /**
     * Bounds for moving the center point
     */
    int minX, maxX, minZ, maxZ, minY, maxY;

    /**
     * Create a CenterPointRenderer at the Origin (0,0,0)
     */
    public CameraCenterPointRenderPlugin() {
        position = new Vector3f(0, 0, 0);
    }

    /**
     * Create a Center Points with Bounds on each coordinate. The Bounds specify
     * the area in which the RenderPoint can be moved.
     * 
     * @param center
     *            Location of Render Point
     * @param maxX
     *            Max X Coordinate for Location
     * @param minX
     *            Min X Coordinate for Location
     * @param maxY
     *            Max Y Coordinate for Location
     * @param minY
     *            Min Y Coordinate for Location
     * @param maxZ
     *            Max Z Coordinate for Location
     * @param minZ
     *            Min Z Coordinate for Location
     */
    public CameraCenterPointRenderPlugin(Vector3f center, int maxX, int minX, int maxY, int minY, int maxZ, int minZ) {

        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.maxY = maxY;
        this.minY = minY;

        position = (Vector3f) center.clone();
    }

    /**
     * Create a CenterPointRenderer with the same Maximum and Minimum Bound on
     * each Coordinate-Axis
     * 
     * @param center
     *            Location of Render Point
     * @param max
     *            Max Value on each Axis for Location
     * @param min
     *            Min Value on each Axis for Location
     */
    public CameraCenterPointRenderPlugin(Vector3f center, int max, int min) {
        this.minX = min;
        this.maxX = max;
        this.minZ = min;
        this.maxZ = max;
        this.maxY = max;
        this.minY = min;

        position = (Vector3f) center.clone();
    }

    /**
     * Get the current position
     * 
     * @return
     */
    public Vector3f getPosition() {
        return position;
    }

    /**
     * Move the Point in the given direction. The direction is first rotated
     * around the specified angle. The angle is delivered by the Camera
     * 
     * @param dir
     *            Direction of movement
     * @param angle
     *            Current Azimuth of the camera
     */
    public void move(Vector3f dir, float angle) {

        if (angle != 0) {
            Matrix3f matrix = new Matrix3f();
            matrix.rotY(angle * MathHelper.DEG2RAD);
            matrix.transform(dir);
        }
        position.add(dir);
        if (position.x < minX) {
            position.x = minX;
        } else if (position.x > maxX) {
            position.x = maxX;
        }
        if (position.y < minY) {
            position.y = minY;
        } else if (position.y > maxY) {
            position.y = maxY;
        }
        if (position.z < minZ) {
            position.z = minZ;
        } else if (position.z > maxZ) {
            position.z = maxZ;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ch.fhnw.jhv.plugins.interfaces.RenderPlugin#render(javax.media.opengl.GL,
     * float)
     */
    public void render(GL gl, float currentTime) {
        gl.glPushMatrix();
        gl.glTranslatef(position.x, position.y, position.z);
        gl.glColor3f(1, 1, 0);
        glut.glutSolidOctahedron();
        gl.glPopMatrix();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.plugins.interfaces.RenderPlugin#getType()
     */
    public RenderPluginType getType() {
        return RenderPluginType.CAMERACENTERPOINT;
    }
}
