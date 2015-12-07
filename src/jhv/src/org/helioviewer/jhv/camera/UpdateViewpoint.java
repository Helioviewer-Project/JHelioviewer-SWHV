package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.time.JHVDate;

abstract class UpdateViewpoint {

    abstract Position.Q update(JHVDate time);
    abstract CameraOptionPanel getOptionPanel();

}
