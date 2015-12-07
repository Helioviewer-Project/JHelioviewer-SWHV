package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.view.View;

class UpdateViewpointExpert extends UpdateViewpoint {

    private final PositionLoad positionLoad;

    UpdateViewpointExpert(PositionLoad _positionLoad) {
        positionLoad = _positionLoad;
    }

    @Override
    Position.Q update(JHVDate time) {
        if (positionLoad.isLoaded()) {
            long currentCameraTime;
            long tLayerStart = 0, tLayerEnd = 0;
            // Active layer times
            View view = Layers.getActiveView();
            if (view != null) {
                tLayerStart = Layers.getStartDate(view).milli;
                tLayerEnd = Layers.getEndDate(view).milli;
            }

            // camera times
            long tPositionStart = positionLoad.getStartTime();
            long tPositionEnd = positionLoad.getEndTime();

            if (tLayerEnd != tLayerStart) {
                currentCameraTime = (long) (tPositionStart + (tPositionEnd - tPositionStart) * (time.milli - tLayerStart) / (double) (tLayerEnd - tLayerStart));
            } else {
                currentCameraTime = tPositionEnd;
            }
            return positionLoad.getInterpolatedPosition(currentCameraTime);
        }
        return Sun.getEarthQuat(time);
    }

}
