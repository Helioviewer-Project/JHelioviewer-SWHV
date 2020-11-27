package org.helioviewer.jhv.layers.connect;

import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.time.JHVTime;

public interface ReceiverConnectivity {

    class Connectivity {

        public final JHVTime time;
        public final List<OrthoScale> SSW;
        public final List<OrthoScale> FSW;
        public final List<OrthoScale> M;

        Connectivity(JHVTime _time, List<OrthoScale> _SSW, List<OrthoScale> _FSW, List<OrthoScale> _M) {
            time = _time;
            SSW = _SSW;
            FSW = _FSW;
            M = _M;
        }

    }

    void setConnectivity(@Nullable Connectivity connectivity);

}
