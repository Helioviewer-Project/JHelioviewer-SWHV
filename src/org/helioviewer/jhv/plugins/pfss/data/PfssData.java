package org.helioviewer.jhv.plugins.pfss.data;

import java.awt.Color;
import java.nio.FloatBuffer;

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

    private final JHVDate dateObs;
    private final short[] fieldlinex;
    private final short[] fieldliney;
    private final short[] fieldlinez;
    private final short[] fieldlines;

    private final double cphi;
    private final double sphi;
    public FloatBuffer vertices;
    public FloatBuffer colors;

    public int lastQuality;
    public boolean lastFixedColor;

    final long time;

    public PfssData(JHVDate _dateObs, short[] _fieldlinex, short[] _fieldliney, short[] _fieldlinez,
            short[] _fieldlines, long _time) {
        dateObs = _dateObs;
        fieldlinex = _fieldlinex;
        fieldliney = _fieldliney;
        fieldlinez = _fieldlinez;
        fieldlines = _fieldlines;
        time = _time;

        Position.L p = Sun.getEarth(dateObs);
        cphi = Math.cos(p.lon);
        sphi = Math.sin(p.lon);

        int numberOfLines = fieldlinex.length / PfssSettings.POINTS_PER_LINE;
        vertices = Buffers.newDirectFloatBuffer(3 * (fieldlinex.length + 2 * numberOfLines));
        colors = Buffers.newDirectFloatBuffer(4 * (fieldlinex.length + 2 * numberOfLines));
    }

    private void addColor(Color color) {
        colors.put(color.getRed() / 255f);
        colors.put(color.getGreen() / 255f);
        colors.put(color.getBlue() / 255f);
        colors.put(color.getAlpha() / 255f);
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

    public boolean needsUpdate(int qualityReduction, boolean fixedColor) {
        return (lastQuality != qualityReduction || lastFixedColor != fixedColor);
    }

    public void calculatePositions(int qualityReduction, boolean fixedColor) {
        lastQuality = PfssSettings.qualityReduction;
        lastFixedColor = PfssSettings.fixedColor;
        vertices.clear();
        colors.clear();

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
        colors.flip();
    }
}
