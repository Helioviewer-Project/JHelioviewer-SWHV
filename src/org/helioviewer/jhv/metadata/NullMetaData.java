package org.helioviewer.jhv.metadata;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.time.JHVTime;

public class NullMetaData extends BaseMetaData {

    public NullMetaData(JHVTime time) {
        viewpoint = Sun.getEarth(time);

        pixelW = 1024;
        pixelH = 1024;
        region = defaultRegion;
    }

}
