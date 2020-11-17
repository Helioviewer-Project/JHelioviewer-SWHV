package org.helioviewer.jhv.layers.connect;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.PositionCartesian;
import org.helioviewer.jhv.time.TimeMap;

public interface ReceiverPositionMap {

    void setPositionMap(@Nullable TimeMap<PositionCartesian> positionMap);

}
