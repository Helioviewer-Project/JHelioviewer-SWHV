package org.helioviewer.jhv.data.datatype.event;

import java.util.List;

import org.helioviewer.jhv.base.math.Vec3;

public class JHVPositionInformation {

    private final List<Vec3> boundBox;
    private final List<Vec3> boundCC;
    private final Vec3 centralPoint;
    public static JHVPositionInformation NULLINFO = new JHVPositionInformation(null, null, null);

    public JHVPositionInformation(List<Vec3> boundBox, List<Vec3> boundCC,
            Vec3 centralPoint) {
        this.boundBox = boundBox;
        this.centralPoint = centralPoint;
        this.boundCC = boundCC;
    }

    public List<Vec3> getBoundBox() {
        return boundBox;
    }

    public Vec3 centralPoint() {
        return centralPoint;
    }

    public List<Vec3> getBoundCC() {
        return boundCC;
    }

}
