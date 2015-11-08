package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Quatd;
import org.helioviewer.jhv.base.time.JHVDate;

public class GL3DEarthCamera extends GL3DCamera {

    @Override
    public void updateRotation(JHVDate date) {
        Position.Latitudinal p = Sun.getEarth(date.getTime());

        localRotation = new Quatd(p.lat, p.lon);
        distance = p.rad;
        updateCameraTransformation();
    }

}
