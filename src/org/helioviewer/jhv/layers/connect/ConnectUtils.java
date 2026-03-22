package org.helioviewer.jhv.layers.connect;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.math.SphericalCoords;
import org.helioviewer.jhv.math.Vec3;

class ConnectUtils {

    static Vec3 toCartesian(String lonStr, String latStr) {
        double lon = Math.toRadians(Double.parseDouble(lonStr));
        double lat = Math.toRadians(Double.parseDouble(latStr));
        return SphericalCoords.unit(lon, lat);
    }

    static Position.Cartesian toCartesian(long milli, String lonStr, String latStr) {
        double lon = Math.toRadians(Double.parseDouble(lonStr));
        double lat = Math.toRadians(Double.parseDouble(latStr));
        return new Position.Cartesian(
                milli,
                SphericalCoords.x(1, lon, lat),
                SphericalCoords.y(1, lat),
                SphericalCoords.z(1, lon, lat));
    }

}
