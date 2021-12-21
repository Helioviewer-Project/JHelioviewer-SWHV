package org.helioviewer.jhv.plugins.pfss.data;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.time.JHVTime;

public class PfssData {

    public final JHVTime dateObs;
    public final float[][] linex;
    public final float[][] liney;
    public final float[][] linez;
    public final float[][] lines;

    private static double decode(short v) {
        return (v + 32768.) * (2. / 65535.) - 1.;
    }

    PfssData(JHVTime _dateObs, short[] _flinex, short[] _fliney, short[] _flinez, short[] _flines, int points) {
        dateObs = _dateObs;

        double elon = Sun.getEarth(dateObs).lon;
        double cphi = Math.cos(elon);
        double sphi = Math.sin(elon);

        int nlines = _flinex.length / points;
        linex = new float[nlines][points];
        liney = new float[nlines][points];
        linez = new float[nlines][points];
        lines = new float[nlines][points];
        for (int j = 0; j < nlines; j++) {
            for (int i = 0; i < points; i++) {
                double x = 3 * decode(_flinex[j * points + i]);
                double y = 3 * decode(_fliney[j * points + i]);
                double z = 3 * decode(_flinez[j * points + i]);

                linex[j][i] = (float) (cphi * x + sphi * y);
                liney[j][i] = (float) (-sphi * x + cphi * y);
                linez[j][i] = (float) z;
                lines[j][i] = (float) MathUtils.clip(decode(_flines[j * points + i]), -1, 1);
            }
        }
    }

}
