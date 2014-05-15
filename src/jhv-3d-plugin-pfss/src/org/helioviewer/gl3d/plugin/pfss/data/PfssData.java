package org.helioviewer.gl3d.plugin.pfss.data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import javax.media.opengl.GL;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;

import org.helioviewer.base.physics.Constants;
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
    private String type = null;
    private String date = null;
    private short[] ptr = null;
    private short[] ptr_nz_len = null;
    private short[] ptph = null;
    private short[] ptth = null;

    private int[] buffer;
    private FloatBuffer vertices;
    private IntBuffer indicesSunToOutside = null;
    private IntBuffer indicesSunToSun = null;
    private IntBuffer indicesOutsideToSun = null;

    private enum TYPE {
        SUN_TO_OUTSIDE, SUN_TO_SUN, OUTSIDE_TO_SUN
    };

    private int VBOVertices;
    private int VBOIndicesSunToOutside;
    private int VBOIndicesSunToSun;
    private int VBOIndicesOutsideToSun;

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
            InputStream is = null;
            try {
                is = new ByteArrayInputStream(gzipFitsFile);
                Fits fits = new Fits(is, true);
                BasicHDU hdus[] = fits.read();
                BinaryTableHDU bhdu = (BinaryTableHDU) hdus[1];
                type = Arrays.toString((String[]) bhdu.getColumn("TYPE"));
                date = Arrays.toString((String[]) bhdu.getColumn("DATE_TIME"));
                ptr = ((short[][]) bhdu.getColumn("PTR"))[0];
                ptr_nz_len = ((short[][]) bhdu.getColumn("PTR_NZ_LEN"))[0];
                ptph = ((short[][]) bhdu.getColumn("PTPH"))[0];
                ptth = ((short[][]) bhdu.getColumn("PTTH"))[0];

                calculatePositions();

            } catch (FitsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    private void createBuffer() {
        vertices = BufferUtil.newFloatBuffer(this.ptr.length * 3 + 3);
        indicesSunToOutside = BufferUtil.newIntBuffer(this.ptr.length * 2);
        indicesOutsideToSun = BufferUtil.newIntBuffer(this.ptr.length * 2);
        indicesSunToSun = BufferUtil.newIntBuffer(this.ptr.length * 2);

    }

    private TYPE getType(int startLine, int lineEnd) {
        if (this.ptr[startLine] > 8192 * 1.05)
            return TYPE.OUTSIDE_TO_SUN;
        else if (this.ptr[lineEnd] > 8192 * 1.05)
            return TYPE.SUN_TO_OUTSIDE;
        else
            return TYPE.SUN_TO_SUN;

    }

    private double calculateAngleBetween2Vectors(double x1, double y1, double z1, double x2, double y2, double z2) {
        return (x1 * x2 + y1 * y2 + z1 * z2) / (Math.sqrt(x1 * x1 + y1 * y1 + z1 * z1) * Math.sqrt(x2 * x2 + y2 * y2 + z2 * z2));
    }

    private void addIndex(TYPE type, int counter) {
        switch (type) {
        case SUN_TO_OUTSIDE:
            indicesSunToOutside.put(counter);
            indicesSunToOutside.put(counter + 1);
            break;
        case SUN_TO_SUN:
            indicesSunToSun.put(counter);
            indicesSunToSun.put(counter + 1);
            break;
        case OUTSIDE_TO_SUN:
            indicesOutsideToSun.put(counter);
            indicesOutsideToSun.put(counter + 1);
            break;
        default:
            break;
        }
    }

    private int addVertex(float x, float y, float z, int counter) {
        vertices.put(x);
        vertices.put(y);
        vertices.put(z);
        return ++counter;
    }

    private void calculatePositions() {
        int lineEnd = this.ptr_nz_len[0] - 1;
        int lineCounter = 1;
        int counter = 0;
        int lineStart = 0;

        this.createBuffer();
        TYPE type = getType(lineStart, lineEnd);

        double xStart = 0;
        double yStart = 0;
        double zStart = 0;
        boolean lineStarted = false;
        for (int i = 0; i < this.ptr.length; i += PfssSettings.LOD_STEPS) {

            if (i > lineEnd && lineCounter < this.ptr_nz_len.length) {
                i = lineEnd;
            }
            boolean colinear = false;

            double r0 = ptr[i] / 8192.0 * Constants.SunRadius;
            double phi0 = ptph[i] / 32768.0 * 2 * Math.PI;
            double theta0 = ptth[i] / 32768.0 * 2 * Math.PI;

            double z = r0 * Math.sin(theta0) * Math.cos(phi0);
            double x = r0 * Math.sin(theta0) * Math.sin(phi0);
            double y = r0 * Math.cos(theta0);

            if (lineStarted) {
                if (i + 1 < ptr.length) {
                    double r1 = ptr[i + 1] / 8192.0 * Constants.SunRadius;
                    double phi1 = ptph[i + 1] / 32768.0 * 2 * Math.PI;
                    double theta1 = ptth[i + 1] / 32768.0 * 2 * Math.PI;

                    double zEnd = r1 * Math.sin(theta1) * Math.cos(phi1);
                    double xEnd = r1 * Math.sin(theta1) * Math.sin(phi1);
                    double yEnd = r1 * Math.cos(theta1);
                    double angle = this.calculateAngleBetween2Vectors(xEnd - x, yEnd - y, zEnd - z, x - xStart, y - yStart, z - zStart);
                    colinear = angle > PfssSettings.ANGLE_OF_LOD && i != lineEnd;
                }
            }

            else {
                lineStarted = true;
                xStart = x;
                yStart = y;
                zStart = z;
            }

            if (!colinear) {
                if (i != lineEnd) {
                    xStart = x;
                    yStart = y;
                    zStart = z;
                    this.addIndex(type, counter);
                }
                counter = this.addVertex((float) x, (float) y, (float) z, counter);

            }

            if (i == lineEnd) {
                lineStarted = false;
                lineStart = lineEnd + 1;
                if (lineCounter < this.ptr_nz_len.length) {
                    lineEnd += this.ptr_nz_len[lineCounter++];
                }

                type = getType(lineStart, lineEnd);
            }

        }
        vertices.flip();
        indicesSunToOutside.flip();
        indicesOutsideToSun.flip();
        indicesSunToSun.flip();
        read = true;
    }

    public String getType() {
        return type;
    }

    public String getDate() {
        return date;
    }

    public short[] getPtr() {
        return ptr;
    }

    public short[] getPtr_nz_len() {
        return ptr_nz_len;
    }

    public short[] getPtph() {
        return ptph;
    }

    public short[] getPtth() {
        return ptth;
    }

    public void init(GL gl) {
        if (gzipFitsFile != null) {
            if (!read)
                readFitsFile();
            if (!init && read && gl != null) {
                buffer = new int[4];
                gl.glGenBuffers(4, buffer, 0);

                VBOVertices = buffer[0];
                gl.glBindBufferARB(GL.GL_ARRAY_BUFFER, VBOVertices);
                gl.glBufferDataARB(GL.GL_ARRAY_BUFFER, vertices.limit() * BufferUtil.SIZEOF_FLOAT, vertices, GL.GL_STATIC_DRAW);
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

                // color
                if (indicesSunToSun != null && indicesSunToSun.limit() > 0) {
                    VBOIndicesSunToSun = buffer[1];
                    gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, VBOIndicesSunToSun);
                    gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, indicesSunToSun.limit() * BufferUtil.SIZEOF_INT, indicesSunToSun, GL.GL_STATIC_DRAW);
                    gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
                }

                if (indicesSunToOutside != null && indicesSunToOutside.limit() > 0) {
                    VBOIndicesSunToOutside = buffer[2];
                    gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, VBOIndicesSunToOutside);
                    gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, indicesSunToOutside.limit() * BufferUtil.SIZEOF_INT, indicesSunToOutside, GL.GL_STATIC_DRAW);
                    gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
                }

                if (indicesOutsideToSun != null && indicesOutsideToSun.limit() > 0) {
                    VBOIndicesOutsideToSun = buffer[3];
                    gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, VBOIndicesOutsideToSun);
                    gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, indicesOutsideToSun.limit() * BufferUtil.SIZEOF_INT, indicesOutsideToSun, GL.GL_STATIC_DRAW);
                    gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
                }

                init = true;
            }
        }
    }

    public void clear(GL gl) {
        if (init) {
            gl.glDeleteBuffers(4, buffer, 0);
        }
    }

    public void display(GL gl) {
        gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
        gl.glDisable(GL.GL_FRAGMENT_PROGRAM_ARB);
        gl.glDisable(GL.GL_VERTEX_PROGRAM_ARB);
        gl.glDisable(GL.GL_LIGHTING);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE);
        gl.glBlendEquation(GL.GL_FUNC_ADD);
        gl.glEnable(GL.GL_LINE_SMOOTH);
        gl.glDepthMask(false);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOVertices);
        gl.glVertexPointer(3, GL.GL_FLOAT, 0, 0);
        GL3DVec3f color;

        gl.glLineWidth(PfssSettings.LINE_WIDTH);
        // gl.glPrimitiveRestartIndexNV(0);

        if (indicesSunToSun != null && indicesSunToSun.limit() > 0) {
            color = PfssSettings.SUN_SUN_LINE_COLOR;
            gl.glColor4f(color.x, color.y, color.z, PfssSettings.LINE_ALPHA);
            gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, VBOIndicesSunToSun);
            gl.glDrawElements(GL.GL_LINES, indicesSunToSun.limit(), GL.GL_UNSIGNED_INT, 0);
        }
        if (indicesSunToOutside != null && indicesSunToOutside.limit() > 0) {
            color = PfssSettings.SUN_OUT_LINE_COLOR;
            gl.glColor4f(color.x, color.y, color.z, PfssSettings.LINE_ALPHA);
            gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, VBOIndicesSunToOutside);
            gl.glDrawElements(GL.GL_LINES, indicesSunToOutside.limit(), GL.GL_UNSIGNED_INT, 0);
        }

        if (indicesOutsideToSun != null && indicesOutsideToSun.limit() > 0) {
            color = PfssSettings.OUT_SUN_LINE_COLOR;
            gl.glColor4f(color.x, color.y, color.z, PfssSettings.LINE_ALPHA);
            gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, VBOIndicesOutsideToSun);
            gl.glDrawElements(GL.GL_LINES, indicesOutsideToSun.limit(), GL.GL_UNSIGNED_INT, 0);
        }
        gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
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