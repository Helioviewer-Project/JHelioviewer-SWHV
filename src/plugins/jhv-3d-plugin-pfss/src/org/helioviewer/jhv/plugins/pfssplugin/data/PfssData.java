package org.helioviewer.jhv.plugins.pfssplugin.data;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.nio.FloatBuffer;
import java.util.Date;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;

import org.helioviewer.base.astronomy.Position;
import org.helioviewer.base.astronomy.Sun;
import org.helioviewer.base.time.TimeUtils;
import org.helioviewer.jhv.plugins.pfssplugin.PfssSettings;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;

/**
 * Loader of fitsfile & VBO generation & OpenGL visualization
 *
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssData {

    private final static Color OPENFIELDCOLOR = Color.RED;
    private final static Color LOOPCOLOR = Color.WHITE;
    private final static Color INSIDEFIELDCOLOR = Color.BLUE;

    private byte[] gzipFitsFile = null;

    private int[] buffer;
    private FloatBuffer vertices;

    private int VBOVertices;
    private int lastQuality;
    public boolean read = false;
    public boolean init = false;

    private boolean lastFixedColor;

    private String dateString;
    private final long time;

    /**
     * Constructor
     *
     * @param time
     * @param url
     */
    public PfssData(byte[] gzipFitsFile, long time) {
        this.gzipFitsFile = gzipFitsFile;
        this.time = time;
    }

    public long getTime() {
        return time;
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
        vertices.put(color.getRed() / 255.f);
        vertices.put(color.getGreen() / 255.f);
        vertices.put(color.getBlue() / 255.f);
        vertices.put(color.getAlpha() / 255.f);
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
            return this.addColor(new Color(1.f, (float) (1. - bright), (float) (1. - bright), opacity), countercolor);
        } else {
            return this.addColor(new Color((float) (1. + bright), (float) (1. + bright), 1.f, opacity), countercolor);
        }
    }

    private void calculatePositions() {
        int counter = 0;
        this.lastQuality = PfssSettings.qualityReduction;
        this.lastFixedColor = PfssSettings.fixedColor;

        double x = 0, y = 0, z = 0;
        int type = 0;
        ByteArrayInputStream is = new ByteArrayInputStream(this.gzipFitsFile);
        try {
            Fits fits = new Fits(is, true);
            BasicHDU hdus[] = fits.read();
            BinaryTableHDU bhdu = (BinaryTableHDU) hdus[1];

            short[] fieldlinex = ((short[]) bhdu.getColumn("FIELDLINEx"));
            short[] fieldliney = ((short[]) bhdu.getColumn("FIELDLINEy"));
            short[] fieldlinez = ((short[]) bhdu.getColumn("FIELDLINEz"));
            short[] fieldlines = ((short[]) bhdu.getColumn("FIELDLINEs"));

            Header header = bhdu.getHeader();

            String date = header.findKey("DATE-OBS");
            dateString = date.substring(11, 30);

            Date dd = TimeUtils.utcDateFormat.parse(dateString);
            Position.Latitudinal p = Sun.getEarth(dd);
            double phi = p.lon;

            double sphi = Math.sin(phi), cphi = Math.cos(phi);

            this.createBuffer(fieldlinex.length);

            for (int i = 0; i < fieldlinex.length; i++) {
                if (i / PfssSettings.POINTS_PER_LINE % 9 <= 8 - PfssSettings.qualityReduction) {
                    int rx = fieldlinex[i] + 32768;
                    int ry = fieldliney[i] + 32768;
                    int rz = fieldlinez[i] + 32768;

                    x = 3. * (rx * 2. / 65535 - 1.);
                    y = 3. * (ry * 2. / 65535 - 1.);
                    z = 3. * (rz * 2. / 65535 - 1.);

                    double helpx = cphi * x + sphi * y;
                    double helpy = -sphi * x + cphi * y;
                    x = helpx;
                    y = helpy;

                    int col = fieldlines[i] + 32768;
                    double bright = (col * 2. / 65535.) - 1.;
                    if (i % PfssSettings.POINTS_PER_LINE == 0) {
                        counter = this.addVertex((float) x, (float) z, (float) -y, counter);
                        counter = this.addColor(bright, 0.f, counter);
                        counter = this.addVertex((float) x, (float) z, (float) -y, counter);
                        if (!PfssSettings.fixedColor) {
                            counter = this.addColor(bright, 1.f, counter);
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
                                counter = this.addColor(PfssData.LOOPCOLOR, counter);
                            } else if (bright < 0) {
                                type = 1;
                                counter = this.addColor(PfssData.INSIDEFIELDCOLOR, counter);
                            } else {
                                type = 2;
                                counter = this.addColor(PfssData.OPENFIELDCOLOR, counter);
                            }
                        }
                    } else if (i % PfssSettings.POINTS_PER_LINE == PfssSettings.POINTS_PER_LINE - 1) {
                        counter = this.addVertex((float) x, (float) z, (float) -y, counter);
                        if (!PfssSettings.fixedColor) {
                            counter = this.addColor(bright, 1.f, counter);
                        } else {
                            if (type == 0) {
                                counter = this.addColor(PfssData.LOOPCOLOR, counter);
                            } else if (type == 1) {
                                counter = this.addColor(PfssData.INSIDEFIELDCOLOR, counter);
                            } else {
                                counter = this.addColor(PfssData.OPENFIELDCOLOR, counter);
                            }
                        }
                        counter = this.addVertex((float) x, (float) z, (float) -y, counter);

                        counter = this.addColor(bright, 0.f, counter);
                    } else {
                        counter = this.addVertex((float) x, (float) z, (float) -y, counter);
                        if (!PfssSettings.fixedColor) {
                            counter = this.addColor(bright, 1.f, counter);
                        } else {
                            if (type == 0) {
                                counter = this.addColor(PfssData.LOOPCOLOR, counter);
                            } else if (type == 1) {
                                counter = this.addColor(PfssData.INSIDEFIELDCOLOR, counter);
                            } else {
                                counter = this.addColor(PfssData.OPENFIELDCOLOR, counter);
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
            if (!init && read && gl != null) {
                buffer = new int[1];
                gl.glGenBuffers(1, buffer, 0);

                VBOVertices = buffer[0];
                gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, VBOVertices);
                gl.glBufferData(GL2.GL_ARRAY_BUFFER, vertices.limit() * Buffers.SIZEOF_FLOAT, vertices, GL2.GL_STATIC_DRAW);
                gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);

                init = true;
            }
        }
    }

    public void clear(GL2 gl) {
        if (init) {
            gl.glDeleteBuffers(1, buffer, 0);
            init = false;
        }
    }

    public void display(GL2 gl) {
        if (PfssSettings.qualityReduction != this.lastQuality || PfssSettings.fixedColor != this.lastFixedColor) {
            this.clear(gl);
            this.init = false;
            this.read = false;
            this.init(gl);
        }
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL2.GL_COLOR_ARRAY);

        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glDepthMask(false);

        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, VBOVertices);
        gl.glColorPointer(4, GL2.GL_FLOAT, 7 * 4, 3 * 4);
        gl.glVertexPointer(3, GL2.GL_FLOAT, 7 * 4, 0);

        gl.glLineWidth(PfssSettings.LINE_WIDTH);
        gl.glDrawArrays(GL2.GL_LINE_STRIP, 0, vertices.limit() / 7);

        gl.glDepthMask(true);
        gl.glEnable(GL2.GL_TEXTURE_2D);

        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
    }

    public boolean isInit() {
        return init;
    }

    public void setInit(boolean init) {
        this.init = init;
    }

    public String getDateString() {
        return dateString;
    }

}
