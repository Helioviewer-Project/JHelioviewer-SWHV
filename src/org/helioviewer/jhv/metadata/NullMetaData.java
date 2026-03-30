package org.helioviewer.jhv.metadata;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.wcs.WcsHeader;

public class NullMetaData extends CommonMetaData {

    public NullMetaData(JHVTime time) {
        viewpoint = Sun.getEarth(time);
        wcsHeader = new WcsHeader(wcsProjection, pv2, wcsPlaneUnitsPerRad, crval, crota);
    }

}
