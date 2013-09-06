package org.helioviewer.gl3d.sceneviewer;

import java.awt.BorderLayout;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;
import javax.swing.JFrame;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.logging.LogSettings;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.scenegraph.GL3DNode;
import org.helioviewer.gl3d.scenegraph.GL3DState;

import com.sun.opengl.util.FPSAnimator;

public abstract class GL3DTestScene implements GLEventListener {

    // public static void main(String[] args) {
    // new GL3DTestScene();
    // }

    private GLCanvas canvas;

    private FPSAnimator animator;

    private GL3DNode sceneRoot;
    private GL3DCamera camera;

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

    public void display(GLAutoDrawable autoDrawable) {
        // Log.debug("Display");
        GL gl = autoDrawable.getGL();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDepthFunc(GL.GL_LEQUAL);
        gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
        // gl.glLoadIdentity();

        // gl.glTranslated(0, 0, -10);

        GL3DState state = GL3DState.getUpdated(gl, 0, 0);

        // gl.glBegin(GL.GL_QUADS);
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

    public void init(GLAutoDrawable autoDrawable) {
        Log.debug("Init");

        GL gl = autoDrawable.getGL();
        gl.glShadeModel(GL.GL_FLAT);
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glDisable(GL.GL_TEXTURE_1D);
        gl.glDisable(GL.GL_TEXTURE_2D);
        gl.glDisable(GL.GL_BLEND);
        // gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        // gl.glEnable(GL.GL_POINT_SMOOTH);

        // gl.glHint(GL.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
        gl.glEnable(GL.GL_COLOR_MATERIAL);

        // enable textures
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);

        gl.glShadeModel(GL.GL_SMOOTH);

        // activate lightning
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_AMBIENT, new float[] { 0.2f, 0.2f, 0.2f }, 0);
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, new float[] { 0.6f, 0.6f, 0.6f }, 0);
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_SPECULAR, new float[] { 0.2f, 0.2f, 0.2f }, 0);
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, new float[] { 0, 0, (float) Constants.SunMeanDistanceToEarth }, 0);
        gl.glEnable(GL.GL_LIGHT0);
        gl.glEnable(GL.GL_LIGHTING);
        gl.glEnable(GL.GL_NORMALIZE);
        gl.glEnable(GL.GL_CULL_FACE);
        gl.glCullFace(GL.GL_BACK);

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

    public void reshape(GLAutoDrawable autoDrawable, int x, int y, int width, int height) {
        System.out.println("Reshape");
        GL gl = autoDrawable.getGL();
        new GLU();
        gl.glViewport(0, 0, width, height);

        // gl.glMatrixMode(GL.GL_PROJECTION);
        // gl.glPushMatrix();
        // gl.glLoadIdentity();
        // glu.gluPerspective(10, ((double)width)/height, 0.1, 120);
        // gl.glMatrixMode(GL.GL_MODELVIEW);
        // gl.glLoadIdentity();
    }

}
