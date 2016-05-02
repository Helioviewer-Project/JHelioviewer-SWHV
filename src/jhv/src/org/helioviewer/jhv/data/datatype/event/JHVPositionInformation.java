package org.helioviewer.jhv.data.datatype.event;

import java.util.List;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.math.Vec3;

public class JHVPositionInformation {

    private final float[] centralPoint;
    private final float[] boundBox;
    private final Position.Q earthPosition;

    public JHVPositionInformation(Vec3 _centralPoint, List<Vec3> _boundBox, List<Vec3> _boundBoxCC, Position.Q p) {
        centralPoint = new float[] { (float) _centralPoint.x, (float) _centralPoint.y, (float) _centralPoint.z };
        List<Vec3> tempboundBox = _boundBoxCC;
        if (_boundBoxCC == null || _boundBoxCC.isEmpty()) {
            tempboundBox = _boundBox;
        }
        if (tempboundBox != null) {
            int len = tempboundBox.size();
            boundBox = new float[3 * len];

            for (int i = 0; i < len; i++) {
                Vec3 pt = tempboundBox.get(i);
                boundBox[3 * i] = (float) pt.x;
                boundBox[3 * i + 1] = (float) pt.y;
                boundBox[3 * i + 2] = (float) pt.z;

            }
        } else {
            boundBox = new float[0];
        }
        earthPosition = p;
    }

    public Vec3 centralPoint() {
        return new Vec3(centralPoint[0], centralPoint[1], centralPoint[2]);
    }

    public float[] getBoundBox() {
        return boundBox;
    }

    public Position.Q getEarthPosition() {
        return earthPosition;
    }

}
