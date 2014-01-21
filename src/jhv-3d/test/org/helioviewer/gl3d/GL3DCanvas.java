package org.helioviewer.gl3d;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;

import org.helioviewer.gl3d.scenegraph.GL3DMesh;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.visuals.GL3DRectangle;

import com.sun.opengl.util.BufferUtil;
import com.sun.opengl.util.FPSAnimator;

public class GL3DCanvas extends GLCanvas implements GLEventListener {

    private static final long serialVersionUID = -6796440052270492736L;

    private GL3DMesh rectangle;

    private int positionVBO;
    private int indexVBO;

    public GL3DCanvas() {

        FPSAnimator animator = new FPSAnimator(this, 10);
        animator.start();

        this.addGLEventListener(this);
    }

    private void createVBO(GL3DState state) {
        FloatBuffer positionBuffer = FloatBuffer.wrap(new float[] { -0.5f, -0.5f, 0, 0.5f, -0.5f, 0, 0.5f, 0.5f, 0, -0.5f, 0.5f, 0 });
        IntBuffer indexBuffer = IntBuffer.wrap(new int[] { 0, 1, 2, 3 });
        //
        // indexBuffer.flip();
        // positionBuffer.flip();

        GL gl = state.gl;
        int[] tmp = new int[1];
        // Create the Vertex Buffer
        gl.glGenBuffers(1, tmp, 0);
        this.positionVBO = tmp[0];
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, this.positionVBO);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, positionBuffer.capacity() * BufferUtil.SIZEOF_FLOAT, positionBuffer, GL.GL_STATIC_DRAW);

        // Create the Indeces Buffer for shell
        gl.glGenBuffers(1, tmp, 0);
        this.indexVBO = tmp[0];
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, this.indexVBO);
        gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.capacity() * BufferUtil.SIZEOF_INT, indexBuffer, GL.GL_STATIC_DRAW);

    }

    /*
     * private void renderVBO(GL3DState state) {
     * 
     * GL gl = state.gl;
     * 
     * gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
     * gl.glBindBuffer(GL.GL_ARRAY_BUFFER, this.positionVBO);
     * gl.glVertexPointer(3, GL.GL_FLOAT, 3 * BufferUtil.SIZEOF_FLOAT, 0);
     * 
     * // Describe to OpenGL where the color data is in the buffer //
     * gl.glEnableClientState(GL.GL_COLOR_ARRAY); //
     * gl.glBindBuffer(GL.GL_COLOR_ARRAY, vbo[0]); // gl.glColorPointer(3,
     * GL.GL_FLOAT, 6 * BufferUtil.SIZEOF_FLOAT, 3 * BufferUtil.SIZEOF_FLOAT);
     * 
     * 
     * // draw all the shells gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER,
     * this.indexVBO); gl.glColor3f(1,1,0);
     * 
     * 
     * // draw the lines gl.glDrawElements(GL.GL_QUADS, 4, GL.GL_UNSIGNED_INT,
     * 0);
     * 
     * gl.glDisableClientState(GL.GL_VERTEX_ARRAY); //
     * gl.glDisableClientState(GL.GL_COLOR_ARRAY);
     * 
     * 
     * // bind with 0, so, switch back to normal pointer operation //
     * gl.glBindBufferARB(GL.GL_ARRAY_BUFFER_ARB, 0); //
     * gl.glBindBufferARB(GL.GL_ELEMENT_ARRAY_BUFFER_ARB, 0); }
     */

    public void display(GLAutoDrawable autoDrawable) {
        System.out.println("Display");
        GL gl = autoDrawable.getGL();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDepthFunc(GL.GL_LEQUAL);
        gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
        gl.glLoadIdentity();

        gl.glTranslated(0, 0, -10);

        GL3DState state = GL3DState.getUpdated(gl, 0, 0);
        //
        // gl.glBegin(GL.GL_QUADS);
        // gl.glVertex3d(-0.5f, -0.5f, -1);
        // gl.glVertex3d(0.5f, -0.5f, -1);
        // gl.glVertex3d(0.5f, 0.5f, -1);
        // gl.glVertex3d(-0.5f, 0.5f, -1);
        // gl.glEnd();

        // gl.glPushM
        this.rectangle.draw(state);
        //
        // this.renderVBO(state);
        state.checkGLErrors();
    }

    public void displayChanged(GLAutoDrawable arg0, boolean arg1, boolean arg2) {
        System.out.println("DisplayChangex");
    }

    public void init(GLAutoDrawable autoDrawable) {
        System.out.println("Init");

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

        // Check VBO SUPPORT
        // VBOHelper.checkVBOSupport(gl);

        // enable textures
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);

        gl.glShadeModel(GL.GL_SMOOTH);

        // activate lightning
        gl.glEnable(GL.GL_LIGHTING);
        gl.glEnable(GL.GL_NORMALIZE);
        gl.glEnable(GL.GL_CULL_FACE);
        gl.glCullFace(GL.GL_BACK);
        // gl.glFrontFace(GL.GL_CCW);

        //
        // gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, new float[]{0.6f, 0.6f,
        // 0.6f}, 0);
        // gl.glLightfv(GL.GL_LIGHT0, GL.GL_AMBIENT, new float[]{0.2f, 0.2f,
        // 0.2f}, 0);
        // gl.glLightfv(GL.GL_LIGHT0, GL.GL_SPECULAR, new float[]{0.6f, 0.6f,
        // 0.6f}, 0);
        // gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, new float[]{0, 35, 10},
        // 0);
        // gl.glEnable(GL.GL_LIGHT0);
        //
        //
        // gl.glLightfv(GL.GL_LIGHT1, GL.GL_DIFFUSE, new float[]{0.6f, 0.6f,
        // 0.6f}, 0);
        // gl.glLightfv(GL.GL_LIGHT1, GL.GL_AMBIENT, new float[]{0.2f, 0.2f,
        // 0.2f}, 0);
        // gl.glLightfv(GL.GL_LIGHT1, GL.GL_SPECULAR, new float[]{0.6f, 0.6f,
        // 0.6f}, 0);
        // gl.glLightfv(GL.GL_LIGHT1, GL.GL_POSITION, new float[]{0, -35, 10},
        // 0);
        // gl.glEnable(GL.GL_LIGHT1);
        //

        GL3DState state = GL3DState.create(gl);

        this.rectangle = new GL3DRectangle(1, 1);
        this.rectangle.init(state);
        createVBO(state);
    }

    public void reshape(GLAutoDrawable autoDrawable, int x, int y, int width, int height) {
        System.out.println("Reshape");
        GL gl = autoDrawable.getGL();
        GLU glu = new GLU();
        gl.glViewport(0, 0, width, height);

        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        glu.gluPerspective(10, ((double) width) / height, 0.1, 120);
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glLoadIdentity();
    };
}
