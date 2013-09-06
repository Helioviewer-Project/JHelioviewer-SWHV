/**
 * 
 */
package ch.fhnw.jhv.gui.viewport.components;

import javax.media.opengl.GL;
import javax.vecmath.Vector3f;

import ch.fhnw.jhv.plugins.interfaces.AbstractPlugin;
import ch.fhnw.jhv.plugins.interfaces.RenderPlugin;

import com.sun.opengl.util.GLUT;

/**
 * SunRenderPlugin is responsible for rendering a sun
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public class SunRenderPlugin extends AbstractPlugin implements RenderPlugin {

    /**
     * Radios of the SUN
     */
    public final static int SUN_RADIUS = 20;

    /**
     * Glut object
     */
    public GLUT glut = new GLUT();

    /**
     * Specify the height of the north- and south pole markers
     */
    private float poleHeight = 3.0f;

    /**
     * Specify the width of the north- and south pole markers
     */
    private float poleWidth = 0.7f;

    /**
     * Render method
     * 
     * @param gl
     *            GL Object
     * @param currentTime
     *            Actual time position
     */
    public void render(GL gl, float currentTime) {

        // if this plugis is active render the sun
        if (this.active) {
            gl.glPushMatrix();
            gl.glColor3d(0.8, 0.7, 0.2);
            drawSphere(gl, SUN_RADIUS, 50, 50);
            drawNorthSouthPole(gl);
            gl.glPopMatrix();
        }
    }

    /**
     * DrawSphere draws a sphere defined by a radius an a number of stacks and
     * slices
     * 
     * @param gl
     *            GL Object
     * @param radius
     *            radius of the sphere
     * @param slices
     *            Number of slices around the z-axis
     * @param stacks
     *            Number of stack along the z-axis
     */
    void drawSphere(GL gl, float radius, int slices, int stacks) {
        float theta, dtheta; // angles around x-axis
        float phi, dphi; // angles around z-axis
        Vector3f n = new Vector3f(); // normal
        int i, j; // loop counters

        // init start values
        theta = 0.0f;
        dtheta = (float) (Math.PI / stacks);
        dphi = 2.0f * (float) (Math.PI / slices);

        // Loop through all visible _stacks
        for (i = 0; i < stacks; ++i) {
            theta = i * dtheta;

            float sin_theta = (float) Math.sin(theta);
            float sin_dtheta = (float) Math.sin(theta + dtheta);
            float cos_theta = (float) Math.cos(theta);
            float cos_dtheta = (float) Math.cos(theta + dtheta);

            gl.glBegin(GL.GL_QUAD_STRIP);
            phi = 0.0f;

            // Loop through all _slices in the current sub _texture
            for (j = 0; j <= slices; ++j) {
                if (j == slices)
                    phi = 0.0f;

                // first quad point
                n.x = sin_theta * (float) Math.cos(phi);
                n.y = sin_theta * (float) Math.sin(phi);
                n.z = cos_theta;

                gl.glNormal3f(n.x, n.y, n.z);

                // multiply with radius to get vertice
                n.scale(radius);
                gl.glVertex3f(n.x, n.y, n.z);

                // second quad point
                n.x = sin_dtheta * (float) Math.cos(phi);
                n.y = sin_dtheta * (float) Math.sin(phi);
                n.z = cos_dtheta;

                gl.glNormal3f(n.x, n.y, n.z);
                n.scale(radius);
                gl.glVertex3f(n.x, n.y, n.z);

                phi += dphi;
            }
            gl.glEnd();
            theta += dtheta;
        }
    }

    /**
     * Draw North and south pole cones to clarify the rotations
     * 
     * @param gl
     *            GL Object
     */
    public void drawNorthSouthPole(GL gl) {

        // DRAW NORTH POLE
        gl.glPushMatrix();
        gl.glTranslatef(0, SUN_RADIUS, 0);
        gl.glRotatef(-90, 1, 0, 0);
        gl.glColor3f(0.9f, 0, 0);
        glut.glutSolidCone(poleWidth, poleHeight, 30, 30);
        gl.glPopMatrix();

        // DRAW SOUTH POLE
        gl.glPushMatrix();
        gl.glTranslatef(0, -SUN_RADIUS, 0);
        gl.glRotatef(90, 1, 0, 0);
        gl.glColor3f(0.1f, 0.1f, 0.1f);
        glut.glutSolidCone(poleWidth, poleHeight, 30, 30);
        gl.glPopMatrix();
    }

    /**
     * Return the type of the Render Plugin
     * 
     * @return RenderPluginType type
     */
    public RenderPluginType getType() {
        return RenderPluginType.SUN;
    }
}
