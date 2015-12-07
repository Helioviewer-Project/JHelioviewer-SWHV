package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.time.JHVDate;

class UpdateViewpointEarth extends UpdateViewpoint {

    @Override
    Position.Q update(JHVDate time) {
        return Sun.getEarthQuat(time);
    }

    @Override
    CameraOptionPanel getOptionPanel() {
        return null;
    }

}
