package org.helioviewer.jhv.layers.connect;

import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.time.JHVTime;

public interface ReceiverConnectivity {

    class Connectivity {

        public final JHVTime time;
        public final List<Vec3> SSW;
        public final List<Vec3> FSW;
        public final List<Vec3> M;

        Connectivity(JHVTime _time, List<Vec3> _SSW, List<Vec3> _FSW, List<Vec3> _M) {
            time = _time;
            SSW = _SSW;
            FSW = _FSW;
            M = _M;
        }

    }

    void setConnectivity(@Nullable Connectivity connectivity);

}
