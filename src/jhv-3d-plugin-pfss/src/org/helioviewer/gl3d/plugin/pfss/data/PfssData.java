package org.helioviewer.gl3d.plugin.pfss.data;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL;

import org.helioviewer.gl3d.plugin.pfss.settings.PfssSettings;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3f;

import com.sun.opengl.util.BufferUtil;

/**
 * Loader of fitsfile & VBO generation & OpenGL visualization
 * 
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssData {
    private byte[] gzipFitsFile = null;
    private final String type = null;
    private final String date = null;
    private final short[] ptr = null;
    private final short[] ptr_nz_len = null;
    private final short[] ptph = null;
    private final short[] ptth = null;

    private int[] buffer;
    private FloatBuffer vertices;
    private FloatBuffer colors;

    private IntBuffer indices = null;

    private int VBOVertices;
    private int VBOColors;
    private int VBOIndices;

    public boolean read = false;
    public boolean init = false;

    /**
     * Constructor
     */
    public PfssData(byte[] gzipFitsFile) {
        this.gzipFitsFile = gzipFitsFile;
    }

    private void readFitsFile() {
        if (gzipFitsFile != null) {
            calculatePositions();
        }
    }

    private void createBuffer(int len) {
        vertices = BufferUtil.newFloatBuffer(len * 4 + 4);
        colors = BufferUtil.newFloatBuffer(len * 4 + 4);
        indices = BufferUtil.newIntBuffer(42 * len);
    }

    private double calculateAngleBetween2Vectors(double x1, double y1, double z1, double x2, double y2, double z2) {
        return (x1 * x2 + y1 * y2 + z1 * z2) / (Math.sqrt(x1 * x1 + y1 * y1 + z1 * z1) * Math.sqrt(x2 * x2 + y2 * y2 + z2 * z2));
    }

    private void addIndex(int counter) {
        indices.put(counter);
        indices.put(counter + 1);
    }

    private int addColor(float x, float y, float z, int counter) {
        colors.put(x);
        colors.put(y);
        colors.put(z);
        return ++counter;
    }

    private int addVertex(float x, float y, float z, int counter) {
        vertices.put(x);
        vertices.put(y);
        vertices.put(z);
        return ++counter;
    }

    private void calculatePositions() {

        int lineEnd = 39;
        int lineCounter = 1;
        int counter = 0;
        int countercolor = 0;

        int lineStart = 0;

        this.createBuffer(this.gzipFitsFile.length / 8);

        double xStart = 0;
        double yStart = 0;
        double zStart = 0;
        boolean lineStarted = false;
        double x = 0, y = 0, z = 0;

        for (int i = 0; i < this.gzipFitsFile.length / 8; i++) {
            int rx = ((this.gzipFitsFile[8 * i + 1] << 8) & 0x0000ff00) | (this.gzipFitsFile[8 * i + 0] & 0x000000ff);
            int ry = ((this.gzipFitsFile[8 * i + 3] << 8) & 0x0000ff00) | (this.gzipFitsFile[8 * i + 2] & 0x000000ff);
            int rz = ((this.gzipFitsFile[8 * i + 5] << 8) & 0x0000ff00) | (this.gzipFitsFile[8 * i + 4] & 0x000000ff);

            x = 3. * (rx * 2. / 65535 - 1.);
            y = 3. * (ry * 2. / 65535 - 1.);
            z = 3. * (rz * 2. / 65535 - 1.);

            if (!lineStarted) {
                lineStarted = true;
                xStart = x;
                yStart = y;
                zStart = z;
            }

            if (i % 40 != 39) {
                xStart = x;
                yStart = y;
                zStart = z;
                this.addIndex(counter);
            }
            counter = this.addVertex((float) x, (float) z, (float) -y, counter);

            int col = ((this.gzipFitsFile[8 * i + 7] << 8) & 0x0000ff00) | (this.gzipFitsFile[8 * i + 6] & 0x000000ff);
            double bright = (col * 2. / 65535.) - 1.;
            if (bright > 0) {
                countercolor = this.addColor(1.f, (float) (1. - bright), (float) (1. - bright), countercolor);
            } else {
                countercolor = this.addColor((float) (1. + bright), (float) (1. + bright), 1.f, countercolor);
            }

            if (i % 40 == 39) {
                lineStarted = false;
                lineStart = lineEnd + 1;
            }

        }
        vertices.flip();
        colors.flip();
        indices.flip();
        read = true;
    }

    public void init(GL gl) {
        if (gzipFitsFile != null) {
            if (!read)
                readFitsFile();
            if (!init && read && gl != null) {
                buffer = new int[3];
                gl.glGenBuffers(3, buffer, 0);

                VBOVertices = buffer[0];
                gl.glBindBufferARB(GL.GL_ARRAY_BUFFER, VBOVertices);
                gl.glBufferDataARB(GL.GL_ARRAY_BUFFER, vertices.limit() * BufferUtil.SIZEOF_FLOAT, vertices, GL.GL_STATIC_DRAW);
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

                // color
                if (indices != null && indices.limit() > 0) {
                    VBOIndices = buffer[1];
                    gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, VBOIndices);
                    gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, indices.limit() * BufferUtil.SIZEOF_INT, indices, GL.GL_STATIC_DRAW);
                    gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
                }
                VBOColors = buffer[2];
                gl.glBindBufferARB(GL.GL_ARRAY_BUFFER, VBOColors);
                gl.glBufferDataARB(GL.GL_ARRAY_BUFFER, colors.limit() * BufferUtil.SIZEOF_FLOAT, colors, GL.GL_STATIC_DRAW);
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
                init = true;
            }
        }
    }

    public void clear(GL gl) {
        if (init) {
            gl.glDeleteBuffers(3, buffer, 0);
        }
    }

    public void display(GL gl) {
        gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL.GL_COLOR_ARRAY);

        gl.glDisable(GL.GL_FRAGMENT_PROGRAM_ARB);
        gl.glDisable(GL.GL_VERTEX_PROGRAM_ARB);
        gl.glDisable(GL.GL_LIGHTING);

        gl.glDisable(GL.GL_TEXTURE_2D);
        gl.glDisable(GL.GL_TEXTURE_1D);

        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE);
        gl.glBlendEquation(GL.GL_FUNC_ADD);
        gl.glEnable(GL.GL_LINE_SMOOTH);
        gl.glDepthMask(false);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOColors);
        gl.glColorPointer(3, GL.GL_FLOAT, 0, 0);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOVertices);
        gl.glVertexPointer(3, GL.GL_FLOAT, 0, 0);
        GL3DVec3f color;

        //gl.glLineWidth(PfssSettings.LINE_WIDTH);
        gl.glLineWidth(0.3f);

        if (indices != null && indices.limit() > 0) {
            color = PfssSettings.SUN_SUN_LINE_COLOR;
            gl.glColor4f(color.x, color.y, color.z, PfssSettings.LINE_ALPHA);
            gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, VBOIndices);
            gl.glDrawElements(GL.GL_LINES, indices.limit(), GL.GL_UNSIGNED_INT, 0);
        }
        gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL.GL_COLOR_ARRAY);

        gl.glDisable(GL.GL_LINE_SMOOTH);
        gl.glDisable(GL.GL_BLEND);
        gl.glDepthMask(true);
        gl.glLineWidth(1f);

    }

    public boolean isInit() {
        return init;
    }

    public void setInit(boolean init) {
        this.init = init;

    }

}