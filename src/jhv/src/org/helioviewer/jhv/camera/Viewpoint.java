package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.base.time.TimeUtils;

public abstract class Viewpoint {

    public JHVDate time;
    public Quat orientation;
    public double distance;

    abstract void update(JHVDate date);
    abstract CameraOptionPanel getOptionPanel();

    Viewpoint() {
        time = TimeUtils.epoch;
        orientation = Quat.ZERO;
        distance = Sun.MeanEarthDistance;
    }

    Viewpoint(Viewpoint v) {
        time = v.time;
        orientation = v.orientation.copy();
        distance = v.distance;
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
