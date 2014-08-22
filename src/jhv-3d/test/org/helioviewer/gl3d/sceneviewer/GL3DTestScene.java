package org.helioviewer.gl3d.sceneviewer;

import java.awt.BorderLayout;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;
import javax.swing.JFrame;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.logging.LogSettings;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.scenegraph.GL3DNode;
import org.helioviewer.gl3d.scenegraph.GL3DState;

import com.jogamp.opengl.util.FPSAnimator;

public abstract class GL3DTestScene implements GLEventListener {

    // public static void main(String[] args) {
    // new GL3DTestScene();
    // }

    private final GLCanvas canvas;

    private final FPSAnimator animator;

    private final GL3DNode sceneRoot;
    private final GL3DCamera camera;

    static {
        LogSettings.init("/log4j.initial.properties", "/log4j.initial.properties", ".", false);
    }

    public GL3DTestScene() {
        JFrame frame = new JFrame("JOGL Frame Test");
        frame.getContentPane().setLayout(new BorderLayout());
        frame.setBounds(100, 100, 600, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // frame.pack();

        this.sceneRoot = getSceneRoot();
        this.camera = getCamera();

        this.canvas = new GLCanvas();
        this.canvas.addGLEventListener(this);

        this.animator = new FPSAnimator(this.canvas, 10);
        this.animator.start();

        frame.getContentPane().add(this.canvas, BorderLayout.CENTER);
        frame.setVisible(true);
        frame.repaint();
    }

    public abstract GL3DNode getSceneRoot();

    public GL3DCamera getCamera() {
        GL3DCamera testCamera = new GL3DTestCamera();
        return testCamera;
    }

    @Override
    public void display(GLAutoDrawable autoDrawable) {
        // Log.debug("Display");
        GL2 gl = (GL2) autoDrawable.getGL();
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glDepthFunc(GL2.GL_LEQUAL);
        gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
        // gl.glLoadIdentity();

        // gl.glTranslated(0, 0, -10);

        GL3DState state = GL3DState.getUpdated(gl, 0, 0);

        // gl.glBegin(GL2.GL_QUADS);
        // gl.glVertex3d(-0.5f, -0.5f, -1);
        // gl.glVertex3d(0.5f, -0.5f, -1);
        // gl.glVertex3d(0.5f, 0.5f, -1);
        // gl.glVertex3d(-0.5f, 0.5f, -1);
        // gl.glEnd();

        // gl.glPushM
        // this.renderVBO(state);
        this.render(state);

        state.checkGLErrors();
    }

    @Override
    public void init(GLAutoDrawable autoDrawable) {
        Log.debug("Init");

        GL2 gl = (GL2) autoDrawable.getGL();
        gl.glShadeModel(GL2.GL_FLAT);
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glDisable(GL2.GL_TEXTURE_1D);
        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glDisable(GL2.GL_BLEND);
        // gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        // gl.glEnable(GL2.GL_POINT_SMOOTH);

        // gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);
        gl.glEnable(GL2.GL_COLOR_MATERIAL);

        // enable textures
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);

        gl.glShadeModel(GL2.GL_SMOOTH);

        // activate lightning
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, new float[] { 0.2f, 0.2f, 0.2f }, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, new float[] { 0.6f, 0.6f, 0.6f }, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, new float[] { 0.2f, 0.2f, 0.2f }, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, new float[] { 0, 0, (float) Constants.SunMeanDistanceToEarth }, 0);
        gl.glEnable(GL2.GL_LIGHT0);
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_NORMALIZE);
        gl.glEnable(GL2.GL_CULL_FACE);
        gl.glCullFace(GL2.GL_BACK);

        GL3DState.create(gl);
        this.camera.activate(null);
        this.canvas.addMouseWheelListener(this.camera.getCurrentInteraction());
        this.canvas.addMouseListener(this.camera.getCurrentInteraction());
        this.canvas.addMouseMotionListener(this.camera.getCurrentInteraction());
    }

    public void render(GL3DState state) {
        state.setActiveChamera(this.camera);

        state.pushMV();
        state.loadIdentity();
        // this.sceneRoot.init(state);
        this.sceneRoot.update(state);
        state.popMV();

        state.pushMV();
        state.getActiveCamera().applyPerspective(state);
        state.getActiveCamera().applyCamera(state);

        this.sceneRoot.draw(state);

        // Draw the camera or its interaction feedbacks
        state.getActiveCamera().drawCamera(state);

        // Resume Previous Projection
        state.getActiveCamera().resumePerspective(state);

        state.popMV();
    }

    public void displayChanged(GLAutoDrawable arg0, boolean arg1, boolean arg2) {
        System.out.println("DisplayChanged");
    }

    @Override
    public void reshape(GLAutoDrawable autoDrawable, int x, int y, int width, int height) {
        System.out.println("Reshape");
        GL2 gl = (GL2) autoDrawable.getGL();
        new GLU();
        gl.glViewport(0, 0, width, height);

        // gl.glMatrixMode(GL2.GL_PROJECTION);
        // gl.glPushMatrix();
        // gl.glLoadIdentity();
        // glu.gluPerspective(10, ((double)width)/height, 0.1, 120);
        // gl.glMatrixMode(GL2.GL_MODELVIEW);
        // gl.glLoadIdentity();
    }

}
