package org.helioviewer.jhv.plugins.pfss.data;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.nio.FloatBuffer;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.opengl.VBO;
import org.helioviewer.jhv.plugins.pfss.PfssSettings;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;

public class PfssData {

    private static final Color OPENFIELDCOLOR = Color.RED;
    private static final Color LOOPCOLOR = Color.WHITE;
    private static final Color INSIDEFIELDCOLOR = Color.BLUE;

    private final byte[] gzipFitsFile;

    private FloatBuffer vertices;

    private VBO vertexVBO;
    private int lastQuality;
    private boolean read = false;
    private boolean init = false;

    private boolean lastFixedColor;

    private String dateObs;
    final long time;

    public PfssData(byte[] _gzipFitsFile, long _time) {
        gzipFitsFile = _gzipFitsFile;
        time = _time;
    }

    private void readFitsFile() {
        if (gzipFitsFile != null) {
            calculatePositions();
        }
    }

    private void createBuffer(int len) {
        int numberOfLines = len / PfssSettings.POINTS_PER_LINE;
        vertices = Buffers.newDirectFloatBuffer(len * (3 + 4) + 2 * numberOfLines * (3 + 4));
    }

    private int addColor(Color color, int counter) {
        vertices.put(color.getRed() / 255f);
        vertices.put(color.getGreen() / 255f);
        vertices.put(color.getBlue() / 255f);
        vertices.put(color.getAlpha() / 255f);
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
            return addColor(new Color(1f, (float) (1. - bright), (float) (1. - bright), opacity), countercolor);
        } else {
            return addColor(new Color((float) (1. + bright), (float) (1. + bright), 1f, opacity), countercolor);
        }
    }

    private void calculatePositions() {
        lastQuality = PfssSettings.qualityReduction;
        lastFixedColor = PfssSettings.fixedColor;

        try (Fits fits = new Fits(new ByteArrayInputStream(gzipFitsFile))) {
            BasicHDU<?> hdus[] = fits.read();
            if (hdus == null || hdus.length < 2 || !(hdus[1] instanceof BinaryTableHDU))
                throw new Exception("Could not read FITS");

            BinaryTableHDU bhdu = (BinaryTableHDU) hdus[1];
            short[] fieldlinex = (short[]) bhdu.getColumn("FIELDLINEx");
            short[] fieldliney = (short[]) bhdu.getColumn("FIELDLINEy");
            short[] fieldlinez = (short[]) bhdu.getColumn("FIELDLINEz");
            short[] fieldlines = (short[]) bhdu.getColumn("FIELDLINEs");

            dateObs = bhdu.getHeader().getStringValue("DATE-OBS");
            if (dateObs == null)
                throw new Exception("DATE-OBS not found");

            createBuffer(fieldlinex.length);

            Position.L p = Sun.getEarth(new JHVDate(dateObs));
            double sphi = Math.sin(p.lon), cphi = Math.cos(p.lon);

            int counter = 0;
            int type = 0;
            for (int i = 0; i < fieldlinex.length; i++) {
                if (i / PfssSettings.POINTS_PER_LINE % 9 <= 8 - PfssSettings.qualityReduction) {
                    int rx = fieldlinex[i] + 32768;
                    int ry = fieldliney[i] + 32768;
                    int rz = fieldlinez[i] + 32768;

                    double x = 3. * (rx * 2. / 65535 - 1.);
                    double y = 3. * (ry * 2. / 65535 - 1.);
                    double z = 3. * (rz * 2. / 65535 - 1.);

                    double helpx = cphi * x + sphi * y;
                    double helpy = -sphi * x + cphi * y;
                    x = helpx;
                    y = helpy;

                    int col = fieldlines[i] + 32768;
                    double bright = (col * 2. / 65535.) - 1.;
                    if (i % PfssSettings.POINTS_PER_LINE == 0) {
                        counter = addVertex((float) x, (float) z, (float) -y, counter);
                        counter = addColor(bright, 0, counter);
                        counter = addVertex((float) x, (float) z, (float) -y, counter);
                        if (!PfssSettings.fixedColor) {
                            counter = addColor(bright, 1, counter);
                        } else {
                            int rox = fieldlinex[i + PfssSettings.POINTS_PER_LINE - 1] + 32768;
                            int roy = fieldliney[i + PfssSettings.POINTS_PER_LINE - 1] + 32768;
                            int roz = fieldlinez[i + PfssSettings.POINTS_PER_LINE - 1] + 32768;
                            double xo = 3. * (rox * 2. / 65535 - 1.);
                            double yo = 3. * (roy * 2. / 65535 - 1.);
                            double zo = 3. * (roz * 2. / 65535 - 1.);
                            double ro = Math.sqrt(xo * xo + yo * yo + zo * zo);
                            double r = Math.sqrt(x * x + y * y + z * z);

                            if (Math.abs(r - ro) < 2.5 - 1.0 - 0.2) {
                                type = 0;
                                counter = addColor(LOOPCOLOR, counter);
                            } else if (bright < 0) {
                                type = 1;
                                counter = addColor(INSIDEFIELDCOLOR, counter);
                            } else {
                                type = 2;
                                counter = addColor(OPENFIELDCOLOR, counter);
                            }
                        }
                    } else if (i % PfssSettings.POINTS_PER_LINE == PfssSettings.POINTS_PER_LINE - 1) {
                        counter = addVertex((float) x, (float) z, (float) -y, counter);
                        if (!PfssSettings.fixedColor) {
                            counter = addColor(bright, 1, counter);
                        } else {
                            if (type == 0) {
                                counter = addColor(LOOPCOLOR, counter);
                            } else if (type == 1) {
                                counter = addColor(INSIDEFIELDCOLOR, counter);
                            } else {
                                counter = addColor(OPENFIELDCOLOR, counter);
                            }
                        }
                        counter = addVertex((float) x, (float) z, (float) -y, counter);
                        counter = addColor(bright, 0, counter);
                    } else {
                        counter = addVertex((float) x, (float) z, (float) -y, counter);
                        if (!PfssSettings.fixedColor) {
                            counter = addColor(bright, 1, counter);
                        } else {
                            if (type == 0) {
                                counter = addColor(LOOPCOLOR, counter);
                            } else if (type == 1) {
                                counter = addColor(INSIDEFIELDCOLOR, counter);
                            } else {
                                counter = addColor(OPENFIELDCOLOR, counter);
                            }
                        }
                    }
                }
            }
            vertices.flip();
            read = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void init(GL2 gl) {
        if (gzipFitsFile != null) {
            if (!read)
                readFitsFile();
            if (!init && read) {
                vertexVBO = new VBO(GL2.GL_ARRAY_BUFFER, -1, 3);
                vertexVBO.init(gl);          
                vertexVBO.bindBufferData(gl, vertices, Buffers.SIZEOF_FLOAT);
                init = true;
            }
        }
    }

    public void clear(GL2 gl) {
        if (init) {
        	vertexVBO.dispose(gl);
            init = false;
        }
    }

    public void display(GL2 gl) {
        if (PfssSettings.qualityReduction != lastQuality || PfssSettings.fixedColor != lastFixedColor) {
            clear(gl);
            init = false;
            read = false;
            init(gl);
        }
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL2.GL_COLOR_ARRAY);

        gl.glDepthMask(false);

        vertexVBO.bindArray(gl);

        gl.glColorPointer(4, GL2.GL_FLOAT, 7 * 4, 3 * 4);
        gl.glVertexPointer(3, GL2.GL_FLOAT, 7 * 4, 0);

        gl.glLineWidth(PfssSettings.LINE_WIDTH);
        gl.glDrawArrays(GL2.GL_LINE_STRIP, 0, vertexVBO.bufferSize);

        vertexVBO.unbindArray(gl);
        gl.glDepthMask(true);

        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
    }

    public boolean isInit() {
        return init;
    }

    public void setInit(boolean _init) {
        init = _init;
    }

    public String getDateObs() {
        return dateObs;
    }

}
