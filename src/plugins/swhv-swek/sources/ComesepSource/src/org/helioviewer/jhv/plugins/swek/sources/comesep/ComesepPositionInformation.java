package org.helioviewer.jhv.plugins.swek.sources.comesep;

import java.util.List;

import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.data.datatype.event.JHVCoordinateSystem;
import org.helioviewer.jhv.data.datatype.event.JHVPositionInformation;

public class ComesepPositionInformation implements JHVPositionInformation {

    @Override
    public JHVCoordinateSystem getCoordinateSystem() {
        return null;
    }

    @Override
    public List<Vec3> getBoundBox() {
        return null;
    }

    @Override
    public Vec3 centralPoint() {
        return null;
    }

    @Override
    public List<Vec3> getBoundCC() {
        return null;
    }

}
