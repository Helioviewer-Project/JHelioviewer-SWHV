package org.helioviewer.jhv.base.scale;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.math.Quat;

public enum GridType {

    Viewpoint, Stonyhurst, Carrington, HCI;

    public Quat toQuat(Position viewpoint) {
        return new Quat(toLatitude(viewpoint), toLongitude(viewpoint));
    }

    public double toLatitude(Position viewpoint) {
        return this == Viewpoint ? viewpoint.lat : 0;
    }

    public double toLongitude(Position viewpoint) {
        switch (this) {
            case Viewpoint:
                return viewpoint.lon;
            case Stonyhurst:
                return 2 * viewpoint.lon - Sun.getEarthHCI(viewpoint.time).lon;
            case Carrington:
                return 2 * viewpoint.lon - Sun.getEarthHCI(viewpoint.time).lon - Sun.getEarth(viewpoint.time).lon;
            default: // HCI
                return 2 * viewpoint.lon;
        }
    }

}
