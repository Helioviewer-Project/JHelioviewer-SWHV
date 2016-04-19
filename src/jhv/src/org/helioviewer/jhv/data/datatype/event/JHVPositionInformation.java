package org.helioviewer.jhv.data.datatype.event;

import java.util.List;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.math.Vec3;

public class JHVPositionInformation {

    private final Vec3 centralPoint;
    private final List<Vec3> boundBox;
    private final List<Vec3> boundCC;
    private final Position.Q earthPosition;

    public JHVPositionInformation(Vec3 centralPoint, List<Vec3> boundBox, List<Vec3> boundCC, Position.Q p) {
        this.centralPoint = centralPoint;
        this.boundBox = boundBox;
        this.boundCC = boundCC;
        earthPosition = p;
    }

    public Vec3 centralPoint() {
        return centralPoint;
    }

    public List<Vec3> getBoundBox() {
        return boundBox;
    }

    public List<Vec3> getBoundCC() {
        return boundCC;
    }

    public Position.Q getEarthPosition() {
        return earthPosition;
    }

}
