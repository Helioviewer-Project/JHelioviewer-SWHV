package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Quatd;
import org.helioviewer.jhv.base.time.JHVDate;

public class VantagePointEarth extends VantagePoint {

    @Override
    protected void update(JHVDate date) {
        time = date;

        Position.Latitudinal p = Sun.getEarth(time.getTime());
        orientation = new Quatd(p.lat, p.lon);
        distance = p.rad;
    }

}
