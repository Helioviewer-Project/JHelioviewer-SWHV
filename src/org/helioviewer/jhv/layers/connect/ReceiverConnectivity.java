package org.helioviewer.jhv.layers.connect;

import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.time.JHVTime;

public interface ReceiverConnectivity {

    class Connectivity {

        public final JHVTime time;
        public final OrthoScaleList SSW;
        public final OrthoScaleList FSW;
        public final OrthoScaleList M;

        Connectivity(JHVTime _time, List<Vec3> cartSSW, List<Vec3> cartFSW, List<Vec3> cartM) {
            time = _time;
            SSW = new OrthoScaleList(cartSSW);
            FSW = new OrthoScaleList(cartFSW);
            M = new OrthoScaleList(cartM);
        }

    }

    void setConnectivity(@Nullable Connectivity connectivity);

}
