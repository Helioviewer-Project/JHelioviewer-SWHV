package org.helioviewer.jhv.plugins.pfss.data;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.time.JHVDate;

public class PfssData {

    private static final float[] openFieldColor = BufferUtils.colorRed;
    private static final float[] loopColor = BufferUtils.colorWhite;
    private static final float[] insideFieldColor = BufferUtils.colorBlue;

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
    private double lastRadius;

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

    public boolean needsUpdate(int detail, boolean fixedColor, double radius) {
        return lastDetail != detail || lastFixedColor != fixedColor || lastRadius != radius;
    }

    private static double decode(short f) {
        return (f + 32768.) * (2. / 65535.) - 1.;
    }

    private void computeColor(float[] color, double b) {
        if (b > 0) {
            color[0] = 1;
            color[1] = (float) (1. - b);
            color[2] = (float) (1. - b);
        } else {
            color[0] = (float) (1. + b);
            color[1] = (float) (1. + b);
            color[2] = 1;
        }
        color[3] = 1;
    }

    public void calculatePositions(int detail, boolean fixedColor, double radius) {
        lastDetail = detail;
        lastFixedColor = fixedColor;
        lastRadius = radius;
        vertices.clear();
        colors.clear();

        float[] oneColor = loopColor;
        float[] brightColor = new float[4];

        for (int i = 0; i < fieldlinex.length; i++) {
            if (i / pointsPerLine % 9 <= detail) {
                double x = 3. * decode(fieldlinex[i]);
                double y = 3. * decode(fieldliney[i]);
                double z = 3. * decode(fieldlinez[i]);
                double b = decode(fieldlines[i]);
                computeColor(brightColor, b);

                double helpx = cphi * x + sphi * y;
                double helpy = -sphi * x + cphi * y;
                x = helpx;
                y = helpy;
                double r = Math.sqrt(x * x + y * y + z * z);

                if (i % pointsPerLine == 0) { // start line
                    BufferUtils.put3f(vertices, (float) x, (float) z, (float) -y);
                    colors.put(BufferUtils.colorNull);

                    if (fixedColor) {
                        double xo = 3. * decode(fieldlinex[i + pointsPerLine - 1]);
                        double yo = 3. * decode(fieldliney[i + pointsPerLine - 1]);
                        double zo = 3. * decode(fieldlinez[i + pointsPerLine - 1]);
                        double ro = Math.sqrt(xo * xo + yo * yo + zo * zo);

                        if (Math.abs(r - ro) < 2.5 - 1.0 - 0.2) {
                            oneColor = loopColor;
                        } else if (b < 0) {
                            oneColor = insideFieldColor;
                        } else {
                            oneColor = openFieldColor;
                        }
                    }
                }

                BufferUtils.put3f(vertices, (float) x, (float) z, (float) -y);
                colors.put(r > radius ? BufferUtils.colorNull : (fixedColor ? oneColor : brightColor));

                if (i % pointsPerLine == pointsPerLine - 1) { // end line
                    BufferUtils.put3f(vertices, (float) x, (float) z, (float) -y);
                    colors.put(BufferUtils.colorNull);
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
