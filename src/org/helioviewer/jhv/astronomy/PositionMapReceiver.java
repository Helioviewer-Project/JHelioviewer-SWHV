package org.helioviewer.jhv.astronomy;

import org.helioviewer.jhv.time.TimeMap;

public interface PositionMapReceiver {

    void setMap(TimeMap<Position> positionMap);

}
