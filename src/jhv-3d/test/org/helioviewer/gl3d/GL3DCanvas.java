package org.helioviewer.gl3d;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;

import org.helioviewer.gl3d.scenegraph.GL3DMesh;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.visuals.GL3DRectangle;

import com.jogamp.common.nio.Buffers;

public class GL3DCanvas extends GLCanvas implements GLEventListener {

    private static final long serialVersionUID = -6796440052270492736L;

    private GL3DMesh rectangle;

    private int positionVBO;
    private int indexVBO;

    public GL3DCanvas() {

        // FPSAnimator animator = new FPSAnimator(this, 10);
        // animator.start();

        this.addGLEventListener(this);
    }

    private void createVBO(GL3DState state) {
        FloatBuffer positionBuffer = FloatBuffer.wrap(new float[] { -0.5f, -0.5f, 0, 0.5f, -0.5f, 0, 0.5f, 0.5f, 0, -0.5f, 0.5f, 0 });
        IntBuffer indexBuffer = IntBuffer.wrap(new int[] { 0, 1, 2, 3 });
        //
        // indexBuffer.flip();
        // positionBuffer.flip();

        GL2 gl = state.gl;
        int[] tmp = new int[1];
        // Create the Vertex Buffer
        gl.glGenBuffers(1, tmp, 0);
        this.positionVBO = tmp[0];
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, this.positionVBO);
        gl.glBufferData(GL2.GL_ARRAY_BUFFER, positionBuffer.capacity() * Buffers.SIZEOF_FLOAT, positionBuffer, GL2.GL_STATIC_DRAW);

        // Create the Indeces Buffer for shell
        gl.glGenBuffers(1, tmp, 0);
        this.indexVBO = tmp[0];
        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, this.indexVBO);
        gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.capacity() * Buffers.SIZEOF_INT, indexBuffer, GL2.GL_STATIC_DRAW);

    }

    /*
     * private void renderVBO(GL3DState state) {
     *
     * GL2 gl = state.gl;
     *
     * gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
     * gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, this.positionVBO);
     * gl.glVertexPointer(3, GL2.GL_FLOAT, 3 * BufferUtil.SIZEOF_FLOAT, 0);
     *
     * // Describe to OpenGL where the color data is in the buffer //
     * gl.glEnableClientState(GL2.GL_COLOR_ARRAY); //
     * gl.glBindBuffer(GL2.GL_COLOR_ARRAY, vbo[0]); // gl.glColorPointer(3,
     * GL2.GL_FLOAT, 6 * BufferUtil.SIZEOF_FLOAT, 3 * BufferUtil.SIZEOF_FLOAT);
     *
     *
     * // draw all the shells gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER,
     * this.indexVBO); gl.glColor3f(1,1,0);
     *
     *
     * // draw the lines gl.glDrawElements(GL2.GL_QUADS, 4, GL2.GL_UNSIGNED_INT,
     * 0);
     *
     * gl.glDisableClientState(GL2.GL_VERTEX_ARRAY); //
     * gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
     *
     *
     * // bind with 0, so, switch back to normal pointer operation //
     * gl.glBindBufferARB(GL2.GL_ARRAY_BUFFER_ARB, 0); //
     * gl.glBindBufferARB(GL2.GL_ELEMENT_ARRAY_BUFFER_ARB, 0); }
     */

    @Override
    public void display(GLAutoDrawable autoDrawable) {
        GL2 gl = (GL2) autoDrawable.getGL();
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glDepthFunc(GL2.GL_LEQUAL);
        gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
        gl.glLoadIdentity();

        gl.glTranslated(0, 0, -10);

        GL3DState state = GL3DState.getUpdated(gl, 0, 0);
        //
        // gl.glBegin(GL2.GL_QUADS);
        // gl.glVertex3d(-0.5f, -0.5f, -1);
        // gl.glVertex3d(0.5f, -0.5f, -1);
        // gl.glVertex3d(0.5f, 0.5f, -1);
        // gl.glVertex3d(-0.5f, 0.5f, -1);
        // gl.glEnd();

        // gl.glPushM
        this.rectangle.draw(state);
        //
        // this.renderVBO(state);
        // state.checkGLErrors();
    }

    public void displayChanged(GLAutoDrawable arg0, boolean arg1, boolean arg2) {
    }

    @Override
    public void init(GLAutoDrawable autoDrawable) {

        GL2 gl = (GL2) autoDrawable.getGL();
        // gl.glShadeModel(GL2.GL_FLAT);
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glDisable(GL2.GL_TEXTURE_1D);
        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glDisable(GL2.GL_BLEND);
        // gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        // gl.glEnable(GL2.GL_POINT_SMOOTH);

        // gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);
        gl.glEnable(GL2.GL_COLOR_MATERIAL);

        // Check VBO SUPPORT
        // VBOHelper.checkVBOSupport(gl);

        // enable textures
        // gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER,
        // GL2.GL_LINEAR);
        // gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER,
        // GL2.GL_LINEAR);

        // gl.glShadeModel(GL2.GL_SMOOTH);

        // activate lightning
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_NORMALIZE);
        gl.glEnable(GL2.GL_CULL_FACE);
        gl.glCullFace(GL2.GL_BACK);
        // gl.glFrontFace(GL2.GL_CCW);

        //
        // gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, new float[]{0.6f, 0.6f,
        // 0.6f}, 0);
        // gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, new float[]{0.2f, 0.2f,
        // 0.2f}, 0);
        // gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, new float[]{0.6f, 0.6f,
        // 0.6f}, 0);
        // gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, new float[]{0, 35, 10},
        // 0);
        // gl.glEnable(GL2.GL_LIGHT0);
        //
        //
        // gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_DIFFUSE, new float[]{0.6f, 0.6f,
        // 0.6f}, 0);
        // gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_AMBIENT, new float[]{0.2f, 0.2f,
        // 0.2f}, 0);
        // gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_SPECULAR, new float[]{0.6f, 0.6f,
        // 0.6f}, 0);
        // gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_POSITION, new float[]{0, -35, 10},
        // 0);
        // gl.glEnable(GL2.GL_LIGHT1);
        //

        GL3DState state = GL3DState.create(gl);

        this.rectangle = new GL3DRectangle(1, 1);
        this.rectangle.init(state);
        createVBO(state);
    }

    @Override
    public void reshape(GLAutoDrawable autoDrawable, int x, int y, int width, int height) {

        GL2 gl = (GL2) autoDrawable.getGL();
        GLU glu = new GLU();
        gl.glViewport(0, 0, width, height);

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        glu.gluPerspective(10, ((double) width) / height, 0.01, 120);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    @Override
    public void dispose(GLAutoDrawable arg0) {
        // TODO Auto-generated method stub

    };
}
