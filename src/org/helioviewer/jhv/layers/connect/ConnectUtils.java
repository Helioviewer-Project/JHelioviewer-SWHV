package org.helioviewer.jhv.layers.connect;

import org.helioviewer.jhv.astronomy.PositionCartesian;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.time.JHVTime;

class ConnectUtils {

    static Vec3 toCartesian(String lonStr, String latStr) {
        double lon = Math.toRadians(Double.parseDouble(lonStr));
        double lat = Math.toRadians(Double.parseDouble(latStr));
        double x = Math.cos(lat) * Math.sin(lon);
        double y = Math.sin(lat);
        double z = Math.cos(lat) * Math.cos(lon);
        return new Vec3(x, y, z);
    }

    static PositionCartesian toCartesian(JHVTime time, String lonStr, String latStr) {
        double lon = Math.toRadians(Double.parseDouble(lonStr));
        double lat = Math.toRadians(Double.parseDouble(latStr));
        double x = Math.cos(lat) * Math.sin(lon);
        double y = Math.sin(lat);
        double z = Math.cos(lat) * Math.cos(lon);
        return new PositionCartesian(time, x, y, z);
    }

}
