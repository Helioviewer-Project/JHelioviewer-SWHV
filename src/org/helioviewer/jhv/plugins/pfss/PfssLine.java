package org.helioviewer.jhv.plugins.pfss;

import java.nio.ShortBuffer;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.plugins.pfss.data.PfssData;

class PfssLine {

    private static final byte[] openFieldColor = Colors.Red;
    private static final byte[] loopColor = Colors.White;
    private static final byte[] insideFieldColor = Colors.Blue;

    private final byte[] brightColor = new byte[4];

    private void computeBrightColor(double b) {
        if (b > 0) {
            byte bb = (byte) (255 * (1. - b));
            brightColor[0] = (byte) 255;
            brightColor[1] = bb;
            brightColor[2] = bb;
        } else {
            byte bb = (byte) (255 * (1. + b));
            brightColor[0] = bb;
            brightColor[1] = bb;
            brightColor[2] = (byte) 255;
        }
        brightColor[3] = (byte) 255;
    }

    private static double decode(ShortBuffer buf, int idx) {
        return (buf.get(idx) + 32768.) * (2. / 65535.) - 1.;
    }

    public void calculatePositions(PfssData data, int detail, boolean fixedColor, double radius, BufVertex lineBuf) {
        int pointsPerLine = data.pointsPerLine;
        double cphi = data.cphi;
        double sphi = data.sphi;
        ShortBuffer flinex = data.flinex;
        ShortBuffer fliney = data.fliney;
        ShortBuffer flinez = data.flinez;
        ShortBuffer flines = data.flines;
        int dlength = flinex.capacity();

        byte[] oneColor = loopColor;
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
                    lineBuf.putVertex((float) x, (float) z, (float) -y, 1, Colors.Null);

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

                lineBuf.putVertex((float) x, (float) z, (float) -y, 1, r > radius ? Colors.Null : (fixedColor ? oneColor : brightColor));

                if (i % pointsPerLine == pointsPerLine - 1) { // end line
                    lineBuf.putVertex((float) x, (float) z, (float) -y, 1, Colors.Null);
                }
            }
        }
    }

}
