package org.helioviewer.jhv.metadata;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.time.JHVTime;

public class NullMetaData extends CommonMetaData {

    public NullMetaData(JHVTime time) {
        viewpoint = Sun.getEarth(time);
    }

}
