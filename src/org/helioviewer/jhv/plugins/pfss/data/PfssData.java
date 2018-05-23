package org.helioviewer.jhv.plugins.pfss.data;

import java.nio.ShortBuffer;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.time.JHVDate;

public class PfssData {

    public final JHVDate dateObs;
    public final int pointsPerLine;
    public final ShortBuffer flinex;
    public final ShortBuffer fliney;
    public final ShortBuffer flinez;
    public final ShortBuffer flines;

    public final double cphi;
    public final double sphi;

    public PfssData(JHVDate _dateObs, short[] _flinex, short[] _fliney, short[] _flinez, short[] _flines, int _pointsPerLine) {
        dateObs = _dateObs;

        int len = _flinex.length;
        flinex = (ShortBuffer) BufferUtils.newShortBuffer(len).put(_flinex).rewind();
        fliney = (ShortBuffer) BufferUtils.newShortBuffer(len).put(_fliney).rewind();
        flinez = (ShortBuffer) BufferUtils.newShortBuffer(len).put(_flinez).rewind();
        flines = (ShortBuffer) BufferUtils.newShortBuffer(len).put(_flines).rewind();

        pointsPerLine = _pointsPerLine;

        double elon = Sun.getEarth(dateObs).lon;
        cphi = Math.cos(elon);
        sphi = Math.sin(elon);
    }

}
