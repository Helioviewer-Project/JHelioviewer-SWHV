package org.helioviewer.jhv.layers.connect;

import java.util.List;

import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.time.JHVTime;

public interface ReceiverConnectivity {

    class Connectivity {

        final JHVTime time;
        final List<Vec3> SSW;
        final List<Vec3> FSW;
        final List<Vec3> M;

        Connectivity(JHVTime _time, List<Vec3> _SSW, List<Vec3> _FSW, List<Vec3> _M) {
            time = _time;
            SSW = _SSW;
            FSW = _FSW;
            M = _M;
        }

    }

    void setConnectivity(Connectivity connectivity);

}
