package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.math.Quat;

public enum GridType {

    Viewpoint, Stonyhurst, Carrington, HCI;

    public Quat toCarrington(Position viewpoint) {
        return this == Viewpoint ? viewpoint.toQuat() : new Quat(0, toLongitude(viewpoint)); // shortcircuit allocation
    }

    public Quat toGrid(Position viewpoint) {
        return new Quat(this == Viewpoint ? -viewpoint.lat : 0, toLongitude(viewpoint));
    }

    public double toLatitude(Position viewpoint) {
        return this == Viewpoint ? viewpoint.lat : 0;
    }

    public double toLongitude(Position viewpoint) {
        switch (this) {
            case Viewpoint:
                return viewpoint.lon;
            case Stonyhurst:
                return Sun.getEarth(viewpoint.time).lon;
            case HCI:
                return Sun.getEarth(viewpoint.time).lon + Sun.getEarthHCI(viewpoint.time).lon;
            default: // Carrington
                return 0;
        }

    }

}
