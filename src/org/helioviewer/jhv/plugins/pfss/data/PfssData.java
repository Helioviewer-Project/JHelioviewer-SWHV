package org.helioviewer.jhv.plugins.pfss.data;

import java.util.Arrays;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.time.JHVTime;

public class PfssData {

    public final JHVTime dateObs;
    public final short[][] linex;
    public final short[][] liney;
    public final short[][] linez;
    public final short[][] lines;

    public final double cphi;
    public final double sphi;

    PfssData(JHVTime _dateObs, short[] _flinex, short[] _fliney, short[] _flinez, short[] _flines, int pointsPerLine) {
        dateObs = _dateObs;

        int nlines = _flinex.length / pointsPerLine;
        linex = new short[nlines][];
        liney = new short[nlines][];
        linez = new short[nlines][];
        lines = new short[nlines][];
        for (int i = 0; i < nlines; i++) {
            linex[i] = Arrays.copyOfRange(_flinex, i * pointsPerLine, (i + 1) * pointsPerLine);
            liney[i] = Arrays.copyOfRange(_fliney, i * pointsPerLine, (i + 1) * pointsPerLine);
            linez[i] = Arrays.copyOfRange(_flinez, i * pointsPerLine, (i + 1) * pointsPerLine);
            lines[i] = Arrays.copyOfRange(_flines, i * pointsPerLine, (i + 1) * pointsPerLine);
        }

        double elon = Sun.getEarth(dateObs).lon;
        cphi = Math.cos(elon);
        sphi = Math.sin(elon);
    }

}
