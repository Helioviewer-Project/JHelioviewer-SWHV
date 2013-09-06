package ch.fhnw.jhv.plugins.vectors.rendering;

import javax.media.opengl.GL;
import javax.vecmath.Vector2f;

import ch.fhnw.jhv.gui.controller.cam.Camera;
import ch.fhnw.jhv.gui.viewport.ViewPort;
import ch.fhnw.jhv.plugins.interfaces.AbstractPlugin;
import ch.fhnw.jhv.plugins.interfaces.RenderPlugin;
import ch.fhnw.jhv.plugins.vectors.data.VectorField;
import ch.fhnw.jhv.plugins.vectors.data.VectorFieldManager;

import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureIO;

/**
 * PlaneRenderPlugin is responsible for Rendering the plane where the vectors
 * will be placed on. The plane contains a texture on the upper area which
 * represents the magnetic field strength. More detailed Description of the
 * Texture you will get in the TextureGenerator class.
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public class PlaneRenderPlugin extends AbstractPlugin implements RenderPlugin {

    /**
     * Width of the plane
     */
    private float width;

    /**
     * Height of the plane
     */
    private float height;

    /**
     * The current scale factor of the plane
     */
    public static float PLANE_SCALE = 0.4f;

    /**
     * The texture on the plane
     */
    private Texture planeTexture;

    /**
     * Is the texture loaded?
     */
    private boolean textureLoaded = false;

    /**
     * Time of the last rendered plane dimension
     */
    private int lastTime = -1;

    /**
     * Current vector field
     */
    private VectorField currentVectorField;

    /**
     * Current active camera
     */
    private Camera activeCamera;

    /**
     * Constructor
     */
    public PlaneRenderPlugin() {

    }

    /**
     * Constructor with params
     * 
     * @param int width
     * @param int height
     */
    public PlaneRenderPlugin(int width, int height) {
        this.width = width * PLANE_SCALE;
        this.height = height * PLANE_SCALE;
    }

    /**
     * Render method
     * 
     * @param GL
     *            gl
     * @param float current time value
     */
    public void render(GL gl, float currentTime) {

        // Load the original field
        VectorField vectorField = VectorFieldManager.getInstance().getOriginalField();

        // if the plugin is active
        if (this.active && currentTime < vectorField.vectors.length) {

            textureLoaded = (vectorField != null);
            /*
             * RELOAD TEXTURE: - if has been loaded a new vector field - if the
             * it has switched to the next time dimension - if there has been
             * set a vector field
             */
            if (vectorField != currentVectorField || (lastTime != (int) currentTime && textureLoaded)) {
                // TEXTURE
                genTexture(gl);

                // Create the plane texture
                planeTexture = TextureIO.newTexture(VectorFieldManager.getInstance().getTextureByTime((int) currentTime), true);

                gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
                gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);

                lastTime = (int) currentTime;

                currentVectorField = vectorField;

                // adapt the size
                setSize((int) currentVectorField.sizePixel.x, (int) currentVectorField.sizePixel.y);
            }

            // If the texture was loaded, apply it on the plane
            if (textureLoaded) {
                // Apply texture.
                planeTexture.enable();
                planeTexture.bind();
            } else {
                gl.glColor3f(1.0f, 0.0f, 0.0f);
            }

            // Create the plane and map the texture on
            gl.glPushMatrix();
            gl.glBegin(GL.GL_POLYGON);
            gl.glColor3f(1.0f, 1.0f, 1.0f);
            gl.glTexCoord2f(0.0f, 0.0f);
            gl.glVertex3f(-width / 2.0f, 0, -height / 2.0f);

            gl.glTexCoord2f(0.0f, 1.0f);
            gl.glVertex3f(-width / 2.0f, 0, height / 2.0f);

            gl.glTexCoord2f(1.0f, 1.0f);
            gl.glVertex3f(width / 2.0f, 0, height / 2.0f);

            gl.glTexCoord2f(1.0f, 0.0f);
            gl.glVertex3f(width / 2.0f, 0, -height / 2.0f);
            gl.glEnd();
            gl.glPopMatrix();

            gl.glDisable(GL.GL_TEXTURE_2D);

            // Create the borders
            float depth = 0.35f;
            gl.glColor3f(0.0f, 0.0f, 0.0f);
            gl.glBegin(GL.GL_POLYGON);
            gl.glVertex3f(-width / 2, 0, height / 2);
            gl.glVertex3f(-width / 2, -depth, height / 2);
            gl.glVertex3f(width / 2, -depth, height / 2);
            gl.glVertex3f(width / 2, 0, height / 2);
            gl.glEnd();

            gl.glBegin(GL.GL_POLYGON);
            gl.glVertex3f(width / 2, 0, -height / 2);
            gl.glVertex3f(width / 2, -depth, -height / 2);
            gl.glVertex3f(-width / 2, -depth, -height / 2);
            gl.glVertex3f(-width / 2, 0, -height / 2);
            gl.glEnd();

            gl.glBegin(GL.GL_POLYGON);
            gl.glVertex3f(width / 2, 0, height / 2);
            gl.glVertex3f(width / 2, -depth, height / 2);
            gl.glVertex3f(width / 2, -depth, -height / 2);
            gl.glVertex3f(width / 2, 0, -height / 2);
            gl.glEnd();

            gl.glBegin(GL.GL_POLYGON);
            gl.glVertex3f(-width / 2, 0, -height / 2);
            gl.glVertex3f(-width / 2, -depth, -height / 2);
            gl.glVertex3f(-width / 2, -depth, height / 2);
            gl.glVertex3f(-width / 2, 0, height / 2);
            gl.glEnd();
        }

    }

    /**
     * Set size
     * 
     * @param width
     * @param heigth
     */
    public void setSize(int width, int height) {
        this.width = width * PLANE_SCALE;
        this.height = height * PLANE_SCALE;
    }

    /**
     * Activate the plugin and set the active camera and set the size
     */

    public void activate() {
        super.activate();

        activeCamera = ViewPort.getInstance().getActiveCamera();

        if (VectorFieldManager.getInstance().getOriginalField() != null) {
            Vector2f size = VectorFieldManager.getInstance().getOriginalField().sizePixel;
            setSize((int) size.x, (int) size.y);
        }
    }

    /**
     * Deactivate the plugin
     */

    public void deactivate() {
        super.deactivate();

        // Set the camera back to the last used active camera
        // Otherwise the bug could occur, that the CenterPoint cam is the active
        // cam
        // in the PFSS Plugin
        if (active)
            ViewPort.getInstance().setActiveCamera(activeCamera);
    }

    /**
     * Last time
     * 
     * @param lastTime
     */
    public void setLastTme(int lastTime) {
        this.lastTime = lastTime;
    }

    /**
     * Generate Textures
     * 
     * @param GL
     *            gl
     * @return int texture id
     */
    private int genTexture(GL gl) {
        final int[] tmp = new int[1];
        gl.glGenTextures(1, tmp, 0);
        return tmp[0];
    }

    /**
     * Return the RenderPluginType
     * 
     * @RenderPluginType type
     */
    public RenderPluginType getType() {
        return RenderPluginType.PLANE;
    }
}
