package org.helioviewer.gl3d.plugin.pfss.data;

import java.nio.FloatBuffer;

import javax.media.opengl.GL;

import org.helioviewer.gl3d.plugin.pfss.settings.PfssSettings;

import com.sun.opengl.util.BufferUtil;

/**
 * Loader of fitsfile & VBO generation & OpenGL visualization
 * 
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssData {
    private byte[] gzipFitsFile = null;

    private int[] buffer;
    private FloatBuffer vertices;

    private int VBOVertices;

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
        int numberOfLines = len / PfssSettings.POINTS_PER_LINE;
        vertices = BufferUtil.newFloatBuffer(len * (3 + 4) + 2 * numberOfLines * (3 + 4));
    }

    private int addColor(float x, float y, float z, float opacity, int counter) {
        vertices.put(x);
        vertices.put(y);
        vertices.put(z);
        vertices.put(opacity);
        return ++counter;
    }

    private int addVertex(float x, float y, float z, int counter) {
        vertices.put(x);
        vertices.put(y);
        vertices.put(z);
        return ++counter;
    }

    private int addColor(double bright, float opacity, int countercolor) {
        if (bright > 0) {
            return this.addColor(1.f, (float) (1. - bright), (float) (1. - bright), opacity, countercolor);
        } else {
            return this.addColor((float) (1. + bright), (float) (1. + bright), 1.f, opacity, countercolor);
        }
    }

    private void calculatePositions() {
        int counter = 0;

        this.createBuffer(this.gzipFitsFile.length / 8);

        double x = 0, y = 0, z = 0;

        for (int i = 0; i < this.gzipFitsFile.length / 8; i++) {
            int rx = ((this.gzipFitsFile[8 * i + 1] << 8) & 0x0000ff00) | (this.gzipFitsFile[8 * i + 0] & 0x000000ff);
            int ry = ((this.gzipFitsFile[8 * i + 3] << 8) & 0x0000ff00) | (this.gzipFitsFile[8 * i + 2] & 0x000000ff);
            int rz = ((this.gzipFitsFile[8 * i + 5] << 8) & 0x0000ff00) | (this.gzipFitsFile[8 * i + 4] & 0x000000ff);

            x = 3. * (rx * 2. / 65535 - 1.);
            y = 3. * (ry * 2. / 65535 - 1.);
            z = 3. * (rz * 2. / 65535 - 1.);
            int col = ((this.gzipFitsFile[8 * i + 7] << 8) & 0x0000ff00) | (this.gzipFitsFile[8 * i + 6] & 0x000000ff);
            double bright = (col * 2. / 65535.) - 1.;

            if (i % PfssSettings.POINTS_PER_LINE == 0) {
                counter = this.addVertex((float) x, (float) z, (float) -y, counter);
                counter = this.addColor(bright, 0.f, counter);
                counter = this.addVertex((float) x, (float) z, (float) -y, counter);
                counter = this.addColor(bright, 1.f, counter);
            } else if (i % PfssSettings.POINTS_PER_LINE == PfssSettings.POINTS_PER_LINE - 1) {
                counter = this.addVertex((float) x, (float) z, (float) -y, counter);
                counter = this.addColor(bright, 1.f, counter);
                counter = this.addVertex((float) x, (float) z, (float) -y, counter);
                counter = this.addColor(bright, 0.f, counter);
            } else {
                counter = this.addVertex((float) x, (float) z, (float) -y, counter);
                counter = this.addColor(bright, 1.f, counter);
            }

        }
        vertices.flip();
        read = true;
    }

    public void init(GL gl) {
        if (gzipFitsFile != null) {
            if (!read)
                readFitsFile();
            if (!init && read && gl != null) {
                buffer = new int[1];
                gl.glGenBuffers(1, buffer, 0);

                VBOVertices = buffer[0];
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOVertices);
                gl.glBufferData(GL.GL_ARRAY_BUFFER, vertices.limit() * BufferUtil.SIZEOF_FLOAT, vertices, GL.GL_STATIC_DRAW);
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

                init = true;
            }
        }
    }

    public void clear(GL gl) {
        if (init) {
            gl.glDeleteBuffers(1, buffer, 0);
        }
    }

    public void display(GL gl) {
        gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL.GL_COLOR_ARRAY);
        gl.glDisableClientState(GL.GL_NORMAL_ARRAY);

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
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOVertices);
        gl.glColorPointer(4, GL.GL_FLOAT, 7 * 4, 3 * 4);
        gl.glVertexPointer(3, GL.GL_FLOAT, 7 * 4, 0);

        gl.glLineWidth(PfssSettings.LINE_WIDTH);
        gl.glDrawArrays(GL.GL_LINE_STRIP, 0, vertices.limit() / 7);

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