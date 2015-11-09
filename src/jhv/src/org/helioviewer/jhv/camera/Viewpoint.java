package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.base.time.TimeUtils;

abstract class Viewpoint {

    JHVDate time;
    Quat orientation;
    double distance;

    abstract void update(JHVDate date);

    Viewpoint() {
        time = TimeUtils.epoch;
        orientation = Quat.ZERO;
        distance = Sun.MeanEarthDistance;
    }

    private JHVDate timeSave;
    private Quat orientationSave;
    private double distanceSave;

    void push() {
        timeSave = time;
        orientationSave = orientation;
        distanceSave = distance;
    }

    void pop() {
        time = timeSave;
        orientation = orientationSave;
        distance = distanceSave;
    }

}
