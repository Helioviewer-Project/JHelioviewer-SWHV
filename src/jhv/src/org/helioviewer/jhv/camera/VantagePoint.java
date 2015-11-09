package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Quatd;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.base.time.TimeUtils;

public abstract class VantagePoint {

    protected JHVDate time;
    protected Quatd orientation;
    protected double distance;

    abstract protected void update(JHVDate date);

    protected VantagePoint() {
        time = TimeUtils.epoch;
        orientation = Quatd.ZERO;
        distance = Sun.MeanEarthDistance;
    }

}
