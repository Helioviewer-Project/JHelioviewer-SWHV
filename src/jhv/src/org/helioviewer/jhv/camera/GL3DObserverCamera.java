package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Quatd;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.View;

public class GL3DObserverCamera extends GL3DCamera {

    @Override
    public void updateRotation(JHVDate date) {
        MetaData m = null;
        View view = Layers.getActiveView();
        if (view != null) {
            m = view.getMetaData(date);
        }

        if (m == null) {
            localRotation = Quatd.ZERO;
            distance = Sun.MeanEarthDistance;
        } else {
            localRotation = m.getRotationObs();
            distance = m.getDistanceObs();
        }
        updateCameraTransformation();
    }

}
