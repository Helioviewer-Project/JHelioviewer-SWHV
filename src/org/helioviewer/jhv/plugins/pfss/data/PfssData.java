package org.helioviewer.jhv.plugins.pfss.data;

import java.awt.Color;
import java.nio.FloatBuffer;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.plugins.pfss.PfssSettings;

public class PfssData {

    private static final float[] red = { Color.RED.getRed() / 255f, Color.RED.getGreen() / 255f, Color.RED.getBlue() / 255f, 1 };
    private static final float[] white = { Color.WHITE.getRed() / 255f, Color.WHITE.getGreen() / 255f, Color.WHITE.getBlue() / 255f, 1 };
    private static final float[] blue = { Color.BLUE.getRed() / 255f, Color.BLUE.getGreen() / 255f, Color.BLUE.getBlue() / 255f, 1 };

    private enum FieldLineColor {
        OPENFIELDCOLOR(red), LOOPCOLOR(white), INSIDEFIELDCOLOR(blue);

        final float[] color;

        FieldLineColor(float[] _color) {
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
    public final FloatBuffer vertices;
    public final FloatBuffer colors;

    private int lastQuality;
    private boolean lastFixedColor;

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
        vertices = BufferUtils.genFloatBuffer(3 * (fieldlinex.length + 2 * numberOfLines));
        colors = BufferUtils.genFloatBuffer(4 * (fieldlinex.length + 2 * numberOfLines));
    }

    private void addColor(double bright) {
        if (bright > 0) {
            BufferUtils.put4f(colors, 1, (float) (1. - bright), (float) (1. - bright), 1);
        } else {
            BufferUtils.put4f(colors, (float) (1. + bright), (float) (1. + bright), 1, 1);
        }
    }

    public boolean needsUpdate(int qualityReduction, boolean fixedColor) {
        return lastQuality != qualityReduction || lastFixedColor != fixedColor;
    }

    private double decode(short f) {
        return (f + 32768.) * (2. / 65535.) - 1.;
    }

    public void calculatePositions(int qualityReduction, boolean fixedColor) {
        lastQuality = qualityReduction;
        lastFixedColor = fixedColor;
        vertices.clear();
        colors.clear();

        FieldLineColor type = FieldLineColor.LOOPCOLOR;
        for (int i = 0; i < fieldlinex.length; i++) {
            if (i / PfssSettings.POINTS_PER_LINE % 9 <= qualityReduction) {
                double x = 3. * decode(fieldlinex[i]);
                double y = 3. * decode(fieldliney[i]);
                double z = 3. * decode(fieldlinez[i]);
                double bright = decode(fieldlines[i]);

                double helpx = cphi * x + sphi * y;
                double helpy = -sphi * x + cphi * y;
                x = helpx;
                y = helpy;

                if (i % PfssSettings.POINTS_PER_LINE == 0) {
                    // start line
                    BufferUtils.put3f(vertices, (float) x, (float) z, (float) -y);
                    BufferUtils.put4f(colors, 0, 0, 0, 0);

                    BufferUtils.put3f(vertices, (float) x, (float) z, (float) -y);
                    if (fixedColor) {
                        double xo = 3. * decode(fieldlinex[i + PfssSettings.POINTS_PER_LINE - 1]);
                        double yo = 3. * decode(fieldliney[i + PfssSettings.POINTS_PER_LINE - 1]);
                        double zo = 3. * decode(fieldlinez[i + PfssSettings.POINTS_PER_LINE - 1]);
                        double ro = Math.sqrt(xo * xo + yo * yo + zo * zo);
                        double r = Math.sqrt(x * x + y * y + z * z);

                        if (Math.abs(r - ro) < 2.5 - 1.0 - 0.2) {
                            type = FieldLineColor.LOOPCOLOR;
                        } else if (bright < 0) {
                            type = FieldLineColor.INSIDEFIELDCOLOR;
                        } else {
                            type = FieldLineColor.OPENFIELDCOLOR;
                        }
                        colors.put(type.color);
                    } else
                        addColor(bright);
                } else {
                    BufferUtils.put3f(vertices, (float) x, (float) z, (float) -y);
                    if (fixedColor)
                        colors.put(type.color);
                    else
                        addColor(bright);
                    // end line
                    if (i % PfssSettings.POINTS_PER_LINE == PfssSettings.POINTS_PER_LINE - 1) {
                        BufferUtils.put3f(vertices, (float) x, (float) z, (float) -y);
                        BufferUtils.put4f(colors, 0, 0, 0, 0);
                    }
                }
            }
        }
        vertices.flip();
        colors.flip();
    }

    public JHVDate getDateObs() {
        return dateObs;
    }

}
