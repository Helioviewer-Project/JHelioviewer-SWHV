package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Quatd;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.View;

public class VantagePointObserver extends VantagePoint {

    @Override
    public void update(JHVDate date) {
        time = date;

        MetaData m = null;
        View view = Layers.getActiveView();
        if (view != null) {
            m = view.getMetaData(time);
        }

        if (m == null) {
            orientation = Quatd.ZERO;
            distance = Sun.MeanEarthDistance;
        } else {
            orientation = m.getRotationObs();
            distance = m.getDistanceObs();
        }
    }

}
