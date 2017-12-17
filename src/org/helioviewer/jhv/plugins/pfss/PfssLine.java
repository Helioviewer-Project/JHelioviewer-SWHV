package org.helioviewer.jhv.plugins.pfss;

import java.nio.FloatBuffer;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.plugins.pfss.data.PfssData;

class PfssLine {

    private static final float[] openFieldColor = BufferUtils.colorRed;
    private static final float[] loopColor = BufferUtils.colorWhite;
    private static final float[] insideFieldColor = BufferUtils.colorBlue;

    static FloatBuffer vertices = BufferUtils.newFloatBuffer(0);
    static FloatBuffer colors = BufferUtils.newFloatBuffer(0);

    private static double decode(short f) {
        return (f + 32768.) * (2. / 65535.) - 1.;
    }

    private static void computeColor(float[] color, double b) {
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

    public static void calculatePositions(PfssData data, int detail, boolean fixedColor, double radius) {
        vertices.clear();
        colors.clear();

        int pointsPerLine = data.pointsPerLine;
        double cphi = data.cphi;
        double sphi = data.sphi;
        short[] fieldlinex = data.fieldlinex;
        short[] fieldliney = data.fieldliney;
        short[] fieldlinez = data.fieldlinez;
        short[] fieldlines = data.fieldlines;

        int numberOfLines = fieldlinex.length / pointsPerLine;
        int vlength = 3 * (fieldlinex.length + 2 * numberOfLines);
        if (vlength != vertices.capacity())
            vertices = BufferUtils.newFloatBuffer(vlength);
        int clength = 4 * (fieldlinex.length + 2 * numberOfLines);
        if (clength != colors.capacity())
            colors = BufferUtils.newFloatBuffer(clength);

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

}
