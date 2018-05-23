package org.helioviewer.jhv.base.scale;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.math.Quat;

public enum GridType {

    Viewpoint, Stonyhurst, Carrington, HCI;

    public Quat getGridQuat(Camera camera) {
        switch (this) {
        case Viewpoint:
            return camera.getViewpoint().orientation;
        case Stonyhurst:
            Position.L p = Sun.getEarth(camera.getViewpoint().time);
            return new Quat(0, p.lon);
        case HCI:
            return Sun.getHCI(camera.getViewpoint().time);
        default: // Carrington
            return Quat.ZERO;
        }
    }

}
