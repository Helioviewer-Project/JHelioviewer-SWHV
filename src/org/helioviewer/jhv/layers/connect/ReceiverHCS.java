package org.helioviewer.jhv.layers.connect;

import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.math.Vec3;

public interface ReceiverHCS {

    class HCS {

        public final List<Vec3> ortho;
        public final List<Vec3> scale;

        HCS(List<Vec3> _ortho, List<Vec3> _scale) {
            ortho = _ortho;
            scale = _scale;
        }

    }

    void setHCS(@Nullable HCS hcs);

}
