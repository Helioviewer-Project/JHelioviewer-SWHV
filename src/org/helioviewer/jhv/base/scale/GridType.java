package org.helioviewer.jhv.base.scale;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.math.Quat;

public enum GridType {

    Viewpoint, Stonyhurst, Carrington, HCI;

    public Quat toQuat(Position viewpoint) {
        switch (this) {
            case Viewpoint:
                return viewpoint.toQuat();
            case Stonyhurst:
                double elon = Sun.getEarth(viewpoint.time).lon;
                return new Quat(0, elon);
            case HCI:
                return Sun.getHCI(viewpoint.time);
            default: // Carrington
                return Quat.ZERO;
        }
    }

}
