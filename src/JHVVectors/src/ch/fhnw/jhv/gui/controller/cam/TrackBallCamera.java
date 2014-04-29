package ch.fhnw.jhv.gui.controller.cam;

import java.awt.event.MouseEvent;

import javax.media.opengl.GL;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import ch.fhnw.jhv.helper.MathHelper;

/**
 * This Camera provides Rotations around 3-Axises. The implementation is based
 * on the teachings of Markus Hudritsch from FHNW.
 * 
 * @author David Hostettler (davidhostettler@gmail.com)
 * @date 14.08.2011
 * 
 */
public class TrackBallCamera extends AbstractZoomCamera {

    /**
     * stores the current rotation of the camera
     */
    private Matrix4f vm;

    /**
     * 
     */
    private Vector3f oldMouseVec = new Vector3f();

    /**
     * Rotation-Axis of previous Rotation. When a new Rotation Axis is calulated
     * it is averaged with the old axis.
     */
    private Vector3f oldAxis;

    /**
     * Last Mouse Position on Screen
     */
    private Vector2f oldMousePos = new Vector2f();

    /**
     * Constructor
     */
    public TrackBallCamera() {
        vm = new Matrix4f();
        vm.setIdentity();
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
        double[] matrix = new double[16];

        // Put Matrix4d back in array in OpenGL order
        for (int row = 0; row < 4; row++)
            for (int col = 0; col < 4; col++)
                matrix[row + col * 4] = vm.getElement(row, col);

        gl.glTranslated(0, 0, -zoom);

        gl.glMultMatrixd(matrix, 0);

    }

    /**
     * Calculate the trackballVector and store it in mv. The TrackballVector is
     * the Point under the X,Y (coordinate on screen) of the "imaginary"
     * trackball.
     * 
     * @param x
     * @param y
     * @param mv
     */
    public void trackballVector(int x, int y, Vector3f mv) {
        // Calculate x & y component to the virtual unit sphere
        float halfSide = (screenWidth < screenHeight ? screenWidth / 2 : screenHeight / 2);
        float vx, vy, vz;
        vx = (x - screenWidth / 2.0f) / halfSide;
        vy = -(y - screenHeight / 2.0f) / halfSide;

        // d = length of vector x,y
        float d = (float) Math.sqrt(vx * vx + vy * vy);

        // z component with pytagoras
        if (d < 1.0f)
            vz = (float) Math.sqrt(1.0f - d * d);
        else
            vz = 0.0f;

        mv.set(vx, vy, vz);
        mv.normalize();
    }

    /**
     * Rotate the View-Matrix
     * 
     * @param angle
     *            Rotation-Angle
     * @param axis
     *            Rotation-Axis
     */
    public void rotate(float angle, Vector3f axis) {
        Matrix4f r = new Matrix4f(); // translation of
                                     // vm
        Vector3f t = new Vector3f(vm.m03, vm.m13, vm.m23); // keep old
                                                           // translation
        r.setIdentity();
        rotateMatrix(r, axis, angle); // build matrix with new rotation
        r.mul(vm);
        vm.set(r);
        vm.m03 = t.x;
        vm.m13 = t.y;
        vm.m23 = t.z;
    }

    /**
     * Rotates the maxtrix around a specific axis
     * 
     * @param matrix
     *            Matrix to be rotated
     * @param axis
     *            Rotation Axis
     * @param angle
     */
    private void rotateMatrix(Matrix4f matrix, Vector3f axis, float angle) {
        float radAngle = angle * MathHelper.DEG2RAD;
        float ca = (float) Math.cos(radAngle), sa = (float) Math.sin(radAngle);
        float l = axis.x * axis.x + axis.y * axis.y + axis.z * axis.z; // length
                                                                       // squared
        float x, y, z;
        x = axis.x;
        y = axis.y;
        z = axis.z;

        if (l > 1.0001f || l < 0.9999f && l != 0) {
            l = 1.0f / (float) Math.sqrt(l);
            x *= l;
            y *= l;
            z *= l;
        }

        float xy = x * y, yz = y * z, xz = x * z, xx = x * x, yy = y * y, zz = z * z;

        matrix.m00 = xx + ca * (1 - xx);
        matrix.m01 = xy - xy * ca - z * sa;
        matrix.m02 = xz - xz * ca + y * sa;
        matrix.m10 = xy - xy * ca + z * sa;
        matrix.m11 = yy + ca * (1 - yy);
        matrix.m12 = yz - yz * ca - x * sa;
        matrix.m20 = xz - xz * ca - y * sa;
        matrix.m21 = yz - yz * ca + x * sa;
        matrix.m22 = zz + ca * (1 - zz);
    }

    public void mousePressed(MouseEvent e) {
        oldMousePos.set(e.getX(), e.getY());
    }

    public void mouseDragged(MouseEvent e) {
        Vector3f curMouseVec = new Vector3f(), axis = new Vector3f(oldMouseVec);
        float angle;
        // calculate current mouse vector at currenct mouse position
        trackballVector(e.getX(), e.getY(), curMouseVec);
        // calculate angle between the old and the current mouse vector
        // Take care that the dot product isn't greater than 1.0 otherwise
        // the acos will return indefined.
        double dot = oldMouseVec.dot(curMouseVec);
        angle = (float) (Math.acos(dot > 1 ? 1 : dot) * (MathHelper.RAD2DEG));
        // calculate rotation axis with the cross product
        axis.cross(oldMouseVec, curMouseVec);

        oldMouseVec.set(curMouseVec);
        Vector2f dMouse = new Vector2f(oldMousePos.x - e.getX(), oldMousePos.y - e.getY());
        float dMouseLenght = dMouse.length();
        if (angle > dMouseLenght)
            angle = dMouseLenght * 0.2f;
        // To stabilise the axis we average it with the last axis
        if (oldAxis != null) {
            axis.add(oldAxis);
            axis.scale(0.5f);
        } else {
            oldAxis = new Vector3f();
        }

        rotate(angle, axis);

        oldAxis.set(axis);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.fhnw.jhv.gui.controller.cam.Camera#getLabel()
     */
    public String getLabel() {
        return "Trackball Camera";
    }
}
