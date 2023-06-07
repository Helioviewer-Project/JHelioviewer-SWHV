package org.helioviewer.jhv.plugins.pfss.data;

import org.helioviewer.jhv.time.JHVTime;

public class PfssData {

    public final JHVTime dateObs;
    public final float[][] lineX;
    public final float[][] lineY;
    public final float[][] lineZ;
    public final float[][] lineS;

    PfssData(JHVTime _dateObs, float[][] _lineX, float[][] _lineY, float[][] _lineZ, float[][] _lineS) {
        dateObs = _dateObs;
        lineX = _lineX;
        lineY = _lineY;
        lineZ = _lineZ;
        lineS = _lineS;
    }

}
