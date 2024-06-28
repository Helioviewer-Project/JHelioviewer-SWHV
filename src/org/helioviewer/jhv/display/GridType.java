package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.math.Quat;

public enum GridType {

    Viewpoint, Stonyhurst, Carrington, HCI;

    public Quat toCarrington(Position viewpoint) {
        return this == Viewpoint ? viewpoint.toQuat() : Quat.createAxisY(toLongitude(viewpoint)); // shortcircuit allocation
    }

    public Quat toGrid(Position viewpoint) {
        return Quat.createXY(this == Viewpoint ? -viewpoint.lat : 0, toLongitude(viewpoint));
    }

    public double toLatitude(Position viewpoint) {
        return this == Viewpoint ? viewpoint.lat : 0;
    }

    public double toLongitude(Position viewpoint) {
        return switch (this) {
            case Viewpoint -> viewpoint.lon;
            case Stonyhurst -> Sun.getEarth(viewpoint.time).lon;
            case Carrington -> 0;
            case HCI -> Sun.getEarth(viewpoint.time).lon + Sun.getEarthHCI(viewpoint.time).lon;
        };

    }

}
