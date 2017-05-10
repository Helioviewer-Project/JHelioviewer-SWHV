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
import org.helioviewer.jhv.plugins.pfss.PfssSettings;

import com.jogamp.common.nio.Buffers;

public class PfssData {

    private enum FieldLineColor {
        OPENFIELDCOLOR(Color.RED), LOOPCOLOR(Color.WHITE), INSIDEFIELDCOLOR(Color.BLUE);

        public final Color color;

        FieldLineColor(Color _color) {
            color = _color;
        }
    }

    private final byte[] gzipFitsFile;

    public FloatBuffer vertices;

    public int lastQuality;
    public boolean lastFixedColor;
    private boolean read;

    private JHVDate dateObs;
    final long time;

    public PfssData(byte[] _gzipFitsFile, long _time) {
        gzipFitsFile = _gzipFitsFile;
        time = _time;
        readFitsFile();
    }

    private void readFitsFile() {
        if (gzipFitsFile != null) {
            calculatePositions(PfssSettings.qualityReduction, PfssSettings.fixedColor);
        }
    }

    private void createBuffer(int len) {
        int numberOfLines = len / PfssSettings.POINTS_PER_LINE;
        vertices = Buffers.newDirectFloatBuffer((3 + 4) * (len + 2 * numberOfLines));
    }

    private void addColor(Color color) {
        vertices.put(color.getRed() / 255f);
        vertices.put(color.getGreen() / 255f);
        vertices.put(color.getBlue() / 255f);
        vertices.put(color.getAlpha() / 255f);
    }

    private void addVertex(float x, float y, float z) {
        vertices.put(x);
        vertices.put(y);
        vertices.put(z);
    }

    private void addColor(double bright, float opacity) {
        if (bright > 0) {
            addColor(new Color(1f, (float) (1. - bright), (float) (1. - bright), opacity));
        } else {
            addColor(new Color((float) (1. + bright), (float) (1. + bright), 1f, opacity));
        }
    }

    public void calculatePositions(int qualityReduction, boolean fixedColor) {
        if (lastQuality == qualityReduction && lastFixedColor == fixedColor)
            return;

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

            String dateFits = bhdu.getHeader().getStringValue("DATE-OBS");
            if (dateFits == null)
                throw new Exception("DATE-OBS not found");
            dateObs = new JHVDate(dateFits);

            createBuffer(fieldlinex.length);

            Position.L p = Sun.getEarth(dateObs);
            double sphi = Math.sin(p.lon), cphi = Math.cos(p.lon);

            FieldLineColor type = FieldLineColor.LOOPCOLOR;
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
                        addVertex((float) x, (float) z, (float) -y);
                        addColor(bright, 0);
                        addVertex((float) x, (float) z, (float) -y);
                        if (!PfssSettings.fixedColor) {
                            addColor(bright, 1);
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
                                type = FieldLineColor.LOOPCOLOR;
                            } else if (bright < 0) {
                                type = FieldLineColor.INSIDEFIELDCOLOR;
                            } else {
                                type = FieldLineColor.OPENFIELDCOLOR;
                            }
                            addColor(type.color);
                        }
                    } else if (i % PfssSettings.POINTS_PER_LINE == PfssSettings.POINTS_PER_LINE - 1) {
                        addVertex((float) x, (float) z, (float) -y);
                        if (!PfssSettings.fixedColor) {
                            addColor(bright, 1);
                        } else {
                            addColor(type.color);
                        }
                        addVertex((float) x, (float) z, (float) -y);
                        addColor(bright, 0);
                    } else {
                        addVertex((float) x, (float) z, (float) -y);
                        if (!PfssSettings.fixedColor) {
                            addColor(bright, 1);
                        } else {
                            addColor(type.color);
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

    public JHVDate getDateObs() {
        return dateObs;
    }

}
