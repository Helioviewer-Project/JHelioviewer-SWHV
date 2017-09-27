package org.helioviewer.jhv.plugins.pfss.data;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.plugins.pfss.PfssSettings;
import org.helioviewer.jhv.time.JHVDate;

public class PfssData {

    private enum FieldLineColor {
        OPENFIELDCOLOR(BufferUtils.colorRed), LOOPCOLOR(BufferUtils.colorWhite), INSIDEFIELDCOLOR(BufferUtils.colorBlue);

        final float[] color;

        FieldLineColor(float[] _color) {
            color = _color;
        }
    }

    private final JHVDate dateObs;
    private final int pointsPerLine;
    private final short[] fieldlinex;
    private final short[] fieldliney;
    private final short[] fieldlinez;
    private final short[] fieldlines;

    private final double cphi;
    private final double sphi;
    public final FloatBuffer vertices;
    public final FloatBuffer colors;

    private int lastDetail;
    private boolean lastFixedColor;

    final long time;

    public PfssData(JHVDate _dateObs, short[] _fieldlinex, short[] _fieldliney, short[] _fieldlinez,
            short[] _fieldlines, int _pointsPerLine, long _time) {
        dateObs = _dateObs;
        fieldlinex = _fieldlinex;
        fieldliney = _fieldliney;
        fieldlinez = _fieldlinez;
        fieldlines = _fieldlines;
        pointsPerLine = _pointsPerLine;
        time = _time;

        Position.L p = Sun.getEarth(dateObs);
        cphi = Math.cos(p.lon);
        sphi = Math.sin(p.lon);

        int numberOfLines = fieldlinex.length / pointsPerLine;
        vertices = BufferUtils.newFloatBuffer(3 * (fieldlinex.length + 2 * numberOfLines));
        colors = BufferUtils.newFloatBuffer(4 * (fieldlinex.length + 2 * numberOfLines));
    }

    private void addColor(double bright) {
        if (bright > 0) {
            BufferUtils.put4f(colors, 1, (float) (1. - bright), (float) (1. - bright), 1);
        } else {
            BufferUtils.put4f(colors, (float) (1. + bright), (float) (1. + bright), 1, 1);
        }
    }

    public boolean needsUpdate(int detail, boolean fixedColor) {
        return lastDetail != detail || lastFixedColor != fixedColor;
    }

    private static double decode(short f) {
        return (f + 32768.) * (2. / 65535.) - 1.;
    }

    public void calculatePositions(int detail, boolean fixedColor) {
        lastDetail = detail;
        lastFixedColor = fixedColor;
        vertices.clear();
        colors.clear();

        FieldLineColor type = FieldLineColor.LOOPCOLOR;
        for (int i = 0; i < fieldlinex.length; i++) {
            if (i / pointsPerLine % 9 <= detail) {
                double x = 3. * decode(fieldlinex[i]);
                double y = 3. * decode(fieldliney[i]);
                double z = 3. * decode(fieldlinez[i]);
                double bright = decode(fieldlines[i]);

                double helpx = cphi * x + sphi * y;
                double helpy = -sphi * x + cphi * y;
                x = helpx;
                y = helpy;

                if (pointsPerLine == 0) {
                    // start line
                    BufferUtils.put3f(vertices, (float) x, (float) z, (float) -y);
                    colors.put(BufferUtils.colorNull);

                    BufferUtils.put3f(vertices, (float) x, (float) z, (float) -y);
                    if (fixedColor) {
                        double xo = 3. * decode(fieldlinex[i + pointsPerLine - 1]);
                        double yo = 3. * decode(fieldliney[i + pointsPerLine - 1]);
                        double zo = 3. * decode(fieldlinez[i + pointsPerLine - 1]);
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
                    if (i % pointsPerLine == pointsPerLine - 1) {
                        BufferUtils.put3f(vertices, (float) x, (float) z, (float) -y);
                        colors.put(BufferUtils.colorNull);
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
