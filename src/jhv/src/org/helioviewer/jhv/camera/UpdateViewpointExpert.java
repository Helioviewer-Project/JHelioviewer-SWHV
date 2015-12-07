package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.view.View;

class UpdateViewpointExpert extends UpdateViewpoint {

    private final Camera camera;
    private final PositionLoad positionLoad = new PositionLoad(this);
    private final CameraOptionPanelExpert expertOptionPanel = new CameraOptionPanelExpert(positionLoad);

    UpdateViewpointExpert(Camera _camera) {
        camera = _camera;
    }

    private Position.Q interpolate(JHVDate time) {
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

            Position.L p = positionLoad.getInterpolatedPosition(currentCameraTime);
            if (p != null) {
                Position.L e = Sun.getEarth(p.time);
                return new Position.Q(p.time, p.rad, new Quat(p.lat, -p.lon + e.lon));
            }
        }

        return Sun.getEarthQuat(time);
    }

    void firePositionLoaded(String state) {
        expertOptionPanel.fireLoaded(state);
        camera.refresh();
    }

    @Override
    Position.Q update(JHVDate date) {
        return interpolate(date);
    }

    @Override
    CameraOptionPanel getOptionPanel() {
        return expertOptionPanel;
    }

}
