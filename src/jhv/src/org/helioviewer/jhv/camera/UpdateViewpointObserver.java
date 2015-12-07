package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.view.View;

class UpdateViewpointObserver extends UpdateViewpoint {

    @Override
    Position.Q update(JHVDate time) {
        View view = Layers.getActiveView();
        if (view == null) {
            return Sun.getEarthQuat(time);
        } else {
            return view.getMetaData(time).getViewpoint();
        }
    }

}
