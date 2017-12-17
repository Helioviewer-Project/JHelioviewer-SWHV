package org.helioviewer.jhv.plugins.pfss.data;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.time.JHVDate;

public class PfssData {

    public final JHVDate dateObs;
    public final int pointsPerLine;
    public final short[] fieldlinex;
    public final short[] fieldliney;
    public final short[] fieldlinez;
    public final short[] fieldlines;

    public final double cphi;
    public final double sphi;

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
    }

}
