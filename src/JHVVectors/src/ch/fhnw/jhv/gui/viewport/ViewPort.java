/**
 * 
 */
package ch.fhnw.jhv.gui.viewport;

import java.awt.Dimension;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLEventListener;

import ch.fhnw.jhv.gui.components.controller.AnimatorController;
import ch.fhnw.jhv.gui.controller.cam.Camera;
import ch.fhnw.jhv.gui.controller.cam.CameraContainer;
import ch.fhnw.jhv.gui.controller.cam.CameraContainer.CameraType;
import ch.fhnw.jhv.plugins.PluginManager;
import ch.fhnw.jhv.plugins.interfaces.RenderPlugin;

import com.sun.opengl.util.FPSAnimator;

/**
 * 
 * ViewPort Class handles all the rendering.
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public class ViewPort extends GLCanvas implements GLEventListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1584269550830147277L;

    /**
     * Frames per second
     */
    public static final int FPS = 30;

    /**
     * Instance to the PluginManager
     */
    private PluginManager pluginManager = PluginManager.getInstance();

    /**
     * FPS Animatir
     */
    private FPSAnimator anim;

    /**
     * OpenGL Object
     */
    private GL gl;

    /**
     * Current active camera
     */
    private Camera cam;

    /**
     * Define if its a projection or not
     */
    private boolean setProjection = false;

    /**
     * Animator Controller is handeling the ticks
     */
    private AnimatorController animatorController;

    /**
     * Holder Class for the singleton pattern
     * 
     * @author Robin Oster (robin.oster@students.fhnw.ch)
     * 
     */
    private static class Holder {
        private static final ViewPort INSTANCE = new ViewPort();
    }

    /**
     * Private Constructor
     */
    private ViewPort() {
        // empty
    }

    /**
     * Get the only existing instance of the ViewPort
     * 
     * @return ViewPort vectorRendererPlugin
     */
    public static ViewPort getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Start the animation
     */
    public void start() {
        anim.start();
    }

    /**
     * Init GL
     * 
     * @param drawable
     *            GLAutoDrawable
     */
    public void init(GLAutoDrawable drawable) {
        // gl options
        gl = drawable.getGL();
        // gl.glClearColor(0.08f, 0.08f, 0.25f, 1.0f);

        float r = 132.0f;
        float g = 157.0f;
        float b = 215.0f;

        gl.glClearColor(r / 255.0f, g / 255.0f, b / 255.0f, 1.0f);
        // gl.glClearColor(1f, 1f, 1f, 1.0f);

        gl.glHint(GL.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
        gl.glEnable(GL.GL_COLOR_MATERIAL);

        // Check VBO SUPPORT
        // VBOHelper.checkVBOSupport(gl);

        // enable textures
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);

        // ------- Beleuchtungsmodell ------------
        // keine Farb-Interpolationen zwischen Vertices
        gl.glShadeModel(GL.GL_SMOOTH);

        // activate lightning
        gl.glEnable(GL.GL_LIGHTING);
        gl.glEnable(GL.GL_NORMALIZE);
        gl.glEnable(GL.GL_CULL_FACE);
        gl.glCullFace(GL.GL_BACK);
        gl.glFrontFace(GL.GL_CCW);

        gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, new float[] { 0.6f, 0.6f, 0.6f }, 0);
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_AMBIENT, new float[] { 0.2f, 0.2f, 0.2f }, 0);
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_SPECULAR, new float[] { 0.6f, 0.6f, 0.6f }, 0);
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, new float[] { 0, 35, 10 }, 0);
        gl.glEnable(GL.GL_LIGHT0);

        gl.glLightfv(GL.GL_LIGHT1, GL.GL_DIFFUSE, new float[] { 0.6f, 0.6f, 0.6f }, 0);
        gl.glLightfv(GL.GL_LIGHT1, GL.GL_AMBIENT, new float[] { 0.2f, 0.2f, 0.2f }, 0);
        gl.glLightfv(GL.GL_LIGHT1, GL.GL_SPECULAR, new float[] { 0.6f, 0.6f, 0.6f }, 0);
        gl.glLightfv(GL.GL_LIGHT1, GL.GL_POSITION, new float[] { 0, -35, 10 }, 0);
        gl.glEnable(GL.GL_LIGHT1);

    }

    /**
     * Reshape the window
     * 
     * @param GLAutoDrawable
     *            drawable
     * @param x
     *            x position of the window
     * @param y
     *            y position of the window
     * @param width
     *            width of the window
     * @param height
     *            height of the window
     */
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        gl = drawable.getGL();
        cam.setProjection(gl, width, height);
    }

    /**
     * Display is responsible for the whole rendering process
     * 
     * @param drawable
     *            GLAutoDrawable
     */
    public void display(GLAutoDrawable drawable) {
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDepthFunc(GL.GL_LEQUAL);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);

        // set the projection again. this needs to be done when the
        // active camera has changed
        if (setProjection) {
            cam.setProjection(gl, this.getWidth(), this.getHeight());
            setProjection = false;
        }

        cam.setView(gl);

        // get the current time from AnimatorController
        float time = animatorController.getCurrentTimestamp();

        // Render all the RenderPlugins from the PluginManager
        for (RenderPlugin renderPlugin : pluginManager.getRenderPlugins()) {
            renderPlugin.render(gl, time);
        }

        animatorController.tick();
    }

    /**
     * Define the current active camera
     * 
     * @param cam
     *            Camera
     */
    public void setActiveCamera(Camera cam) {

        if (this.cam != null) {
            this.cam.dettach();
            this.removeMouseListener(this.cam);
            this.removeMouseMotionListener(this.cam);
            this.removeMouseWheelListener(this.cam);
            this.removeKeyListener(this.cam);
        }

        this.cam = cam;

        this.addMouseListener(cam);
        this.addMouseMotionListener(cam);
        this.addMouseWheelListener(cam);
        this.addKeyListener(cam);

        setProjection = true;
    }

    /**
     * Return the current active camera
     * 
     * @return camera Camera
     */
    public Camera getActiveCamera() {
        return this.cam;
    }

    /**
     * Display has changed
     * 
     * @param drawable
     *            GLAutoDrawable
     * @param arg1
     *            booelan
     * @param arg2
     *            boolean
     */
    public void displayChanged(GLAutoDrawable drawable, boolean arg1, boolean arg2) {
        // TODO Auto-generated method stub

    }

    /**
     * Create the animator class and define the size
     * 
     * @param width
     *            animator width
     * @param height
     *            animator height
     * @param animator
     *            AnimatorController
     */
    public void createAnimator(int width, int height, AnimatorController animator) {
        this.addGLEventListener(this);
        this.setMinimumSize(new Dimension(width, height));
        this.setSize(new Dimension(width, height));

        // set the animatorcontroller class
        animatorController = animator;

        // start animation
        anim = new FPSAnimator(this, FPS, true);

        // create trackball camera
        this.setActiveCamera(CameraContainer.getCamera(CameraType.ROTATION_SUN));
    }
}
