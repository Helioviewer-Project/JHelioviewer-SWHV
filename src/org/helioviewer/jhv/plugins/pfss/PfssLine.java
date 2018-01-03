package org.helioviewer.jhv.plugins.pfss;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.opengl.GLLine;
import org.helioviewer.jhv.plugins.pfss.data.PfssData;

import com.jogamp.opengl.GL2;

class PfssLine {

    private static final double thickness = 0.004;
    private static final float[] openFieldColor = BufferUtils.colorRed;
    private static final float[] loopColor = BufferUtils.colorWhite;
    private static final float[] insideFieldColor = BufferUtils.colorBlue;

    private final GLLine line = new GLLine();
    private final float[] brightColor = new float[4];
    private FloatBuffer vertices = BufferUtils.newFloatBuffer(0);
    private FloatBuffer colors = BufferUtils.newFloatBuffer(0);

    void init(GL2 gl) {
        line.init(gl);
    }

    void dispose(GL2 gl) {
        line.dispose(gl);
    }

    void render(GL2 gl, double aspect) {
        line.render(gl, aspect, thickness);
    }

    private void computeBrightColor(double b) {
        if (b > 0) {
            brightColor[0] = 1;
            brightColor[1] = (float) (1. - b);
            brightColor[2] = (float) (1. - b);
        } else {
            brightColor[0] = (float) (1. + b);
            brightColor[1] = (float) (1. + b);
            brightColor[2] = 1;
        }
        brightColor[3] = 1;
    }

    private static double decode(ShortBuffer buf, int idx) {
        return (buf.get(idx) + 32768.) * (2. / 65535.) - 1.;
    }

    public void calculatePositions(GL2 gl, PfssData data, int detail, boolean fixedColor, double radius) {
        vertices.clear();
        colors.clear();

        int pointsPerLine = data.pointsPerLine;
        double cphi = data.cphi;
        double sphi = data.sphi;
        ShortBuffer flinex = data.flinex;
        ShortBuffer fliney = data.fliney;
        ShortBuffer flinez = data.flinez;
        ShortBuffer flines = data.flines;

        int dlength = flinex.capacity();
        int numberOfLines = dlength / pointsPerLine;

        int vlength = 3 * (dlength + 2 * numberOfLines);
        if (vlength != vertices.capacity())
            vertices = BufferUtils.newFloatBuffer(vlength);

        int clength = 4 * (dlength + 2 * numberOfLines);
        if (clength != colors.capacity())
            colors = BufferUtils.newFloatBuffer(clength);

        float[] oneColor = loopColor;
        for (int i = 0; i < dlength; i++) {
            if (i / pointsPerLine % 9 <= detail) {
                double x = 3. * decode(flinex, i);
                double y = 3. * decode(fliney, i);
                double z = 3. * decode(flinez, i);
                double b = decode(flines, i);
                computeBrightColor(b);

                double helpx = cphi * x + sphi * y;
                double helpy = -sphi * x + cphi * y;
                x = helpx;
                y = helpy;
                double r = Math.sqrt(x * x + y * y + z * z);

                if (i % pointsPerLine == 0) { // start line
                    BufferUtils.put3f(vertices, (float) x, (float) z, (float) -y);
                    colors.put(BufferUtils.colorNull);

                    if (fixedColor) {
                        double xo = 3. * decode(flinex, i + pointsPerLine - 1);
                        double yo = 3. * decode(fliney, i + pointsPerLine - 1);
                        double zo = 3. * decode(flinez, i + pointsPerLine - 1);
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

        line.setData(gl, vertices, colors);
    }

}
