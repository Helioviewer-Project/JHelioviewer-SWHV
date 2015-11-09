package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.View;

class VantagePointObserver extends VantagePoint {

    @Override
    void update(JHVDate date) {
        time = date;

        View view = Layers.getActiveView();
        if (view == null) {
            orientation = Quat.ZERO;
            distance = Sun.MeanEarthDistance;
        } else {
            MetaData m = view.getMetaData(time);
            orientation = m.getRotationObs();
            distance = m.getDistanceObs();
        }
    }

}
