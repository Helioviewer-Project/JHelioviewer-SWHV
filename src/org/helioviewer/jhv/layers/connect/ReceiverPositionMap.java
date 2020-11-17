package org.helioviewer.jhv.layers.connect;

import org.helioviewer.jhv.astronomy.PositionCartesian;
import org.helioviewer.jhv.time.TimeMap;

public interface ReceiverPositionMap {

    void setPositionMap(TimeMap<PositionCartesian> positionMap);

}
