package org.helioviewer.jhv.plugins.pfss;

import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.plugins.pfss.data.PfssData;

class PfssLine {

    private static final byte[] openFieldColor = Colors.Red;
    private static final byte[] loopColor = Colors.White;
    private static final byte[] insideFieldColor = Colors.Blue;

    private static void computeBrightColor(double b, byte[] brightColor) {
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

    private static double decode(short v) {
        return (v + 32768.) * (2. / 65535.) - 1.;
    }

    static void calculatePositions(PfssData data, int detail, boolean fixedColor, double radius, BufVertex lineBuf) {
        double cphi = data.cphi;
        double sphi = data.sphi;
        short[][] linex = data.linex;
        short[][] liney = data.liney;
        short[][] linez = data.linez;
        short[][] lines = data.lines;
        int nlines = linex.length;
        int points = linex[0].length;

        byte[] brightColor = new byte[4];
        byte[] oneColor = loopColor;

        for (int j = 0; j < nlines; j++) {
            if (j % (PfssSettings.MAX_DETAIL + 1) <= detail) {
                for (int i = 0; i < points; i++) {
                    double x = 3 * decode(linex[j][i]);
                    double y = 3 * decode(liney[j][i]);
                    double z = 3 * decode(linez[j][i]);
                    double b = MathUtils.clip(decode(lines[j][i]), -1, 1);
                    computeBrightColor(b, brightColor);

                    double helpx = cphi * x + sphi * y;
                    double helpy = -sphi * x + cphi * y;
                    x = helpx;
                    y = helpy;
                    double r = Math.sqrt(x * x + y * y + z * z);

                    if (i == 0) {
                        lineBuf.putVertex((float) x, (float) z, (float) -y, 1, Colors.Null);

                        if (fixedColor) {
                            double xo = 3 * decode(linex[j][points - 1]);
                            double yo = 3 * decode(liney[j][points - 1]);
                            double zo = 3 * decode(linez[j][points - 1]);
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
                    if (i == points - 1) {
                        lineBuf.repeatVertex(Colors.Null);
                    }
                }
            }
        }
    }

}
