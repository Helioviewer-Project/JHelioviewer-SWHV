package org.helioviewer.swhv;

import java.awt.Component;

import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;

import org.helioviewer.gl3d.scenegraph.math.GL3DMat4f;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3f;
import org.helioviewer.swhv.objects3d.Cube;
import org.helioviewer.swhv.objects3d.SolarObject;
import org.helioviewer.swhv.time.GlobalTimeListener;

public class GL3DPanel implements GLEventListener, GlobalTimeListener {

    Camera camera;
    private GLCanvas canvas;
    private Trackball tb;
    private Cube cube;
    public static final SolarObject sun = new SolarObject();

    public GL3DPanel() {
        this.initCanvas();
        this.camera = new DefaultCamera(90.f, 1.f, 4.f, new GL3DVec3f(0f, 0f, 2f), new GL3DVec3f(1.f, 0.f, 0.f), new GL3DVec3f(0.f, 1.f, 0.f));
    }

    private void initCanvas() {
        GLProfile profile = GLProfile.getGL2GL3();
        GLCapabilities capabilities = new GLCapabilities(profile);
        canvas = new GLCanvas(capabilities);
        canvas.addGLEventListener(this);
        tb = new Trackball();
        tb.trackballReshape(canvas.getWidth() / 4, canvas.getHeight() / 4);
        tb.trackballInit(0);

        this.canvas.addMouseMotionListener(tb);
        this.canvas.addMouseListener(tb);
        this.canvas.setAutoSwapBufferMode(false);
        this.canvas.display();
    }

    static int count = 0;
    GL3DMat4f localmat = GL3DMat4f.identity();

    @Override
    public void display(GLAutoDrawable glad) {

        GL3 gl = (GL3) glad.getGL();
        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        gl.glClear(GL3.GL_COLOR_BUFFER_BIT);
        GL3DMat4f vv = new GL3DMat4f(tb.trackballMatrix());
        GL3DMat4f mat = this.camera.getViewProjectionMatrix(gl);
        GL3DMat4f localmatinverse = localmat.inverse();
        mat.multiply(localmatinverse);
        localmat = vv.multiply(localmat);
        mat.multiply(vv);
        this.cube.render(gl, mat.m);
        this.sun.render(gl, mat.m);
        glad.swapBuffers();
    }

    @Override
    public void dispose(GLAutoDrawable glad) {

    }

    @Override
    public void init(GLAutoDrawable glad) {

        GL3 gl = (GL3) glad.getGL();
        int[] viewport = new int[4];
        gl.glGetIntegerv(GL3.GL_VIEWPORT, viewport, 0);
        int width = viewport[2];
        int height = viewport[3];
        gl.glViewport(0, 0, width, height);
        this.cube = new Cube();
        this.cube.initializeObject(gl);
        this.sun.initializeObject(gl);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL3 gl = drawable.getGL().getGL3();
        tb.trackballReshape(width / 4, height / 4);
        gl.glViewport(x, y, width, height);
        this.getCamera().reshape(width, height);
    }

    public Camera getCamera() {
        return this.camera;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public Component getCanvas() {
        return this.canvas;
    }

    public SolarObject getSolarObject() {
        return this.sun;
    }

    @Override
    public void beginTimeChanged(long beginTime) {
    }

    @Override
    public void endTimeChanged(long endTime) {
    }

    @Override
    public void currentTimeChanged(long endTime) {
        canvas.display();
    }
}
