package org.helioviewer.jhv.plugins.pfss.data;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.time.JHVDate;

public class PfssData {

    public final JHVDate dateObs;
    public final int pointsPerLine;
    public final short[] flinex;
    public final short[] fliney;
    public final short[] flinez;
    public final short[] flines;

    public final double cphi;
    public final double sphi;

    public PfssData(JHVDate _dateObs, short[] _flinex, short[] _fliney, short[] _flinez, short[] _flines, int _pointsPerLine) {
        dateObs = _dateObs;
        flinex = _flinex;
        fliney = _fliney;
        flinez = _flinez;
        flines = _flines;
        pointsPerLine = _pointsPerLine;

        Position.L p = Sun.getEarth(dateObs);
        cphi = Math.cos(p.lon);
        sphi = Math.sin(p.lon);
    }

}
