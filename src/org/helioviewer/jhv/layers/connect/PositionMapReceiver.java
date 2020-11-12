package org.helioviewer.jhv.layers.connect;

import org.helioviewer.jhv.astronomy.PositionCartesian;
import org.helioviewer.jhv.time.TimeMap;

public interface PositionMapReceiver {

    void setMap(TimeMap<PositionCartesian> positionMap);

}
