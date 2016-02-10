package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.view.View;

public interface UpdateViewpoint {

    Position.Q update(JHVDate time);

    public static UpdateViewpoint updateEarthInertial = new UpdateViewpointEarthInertial();
    public static UpdateViewpoint updateEarth = new UpdateViewpointEarth();
    public static UpdateViewpoint updateExpert = new UpdateViewpointExpert();
    public static UpdateViewpoint updateObserver = new UpdateViewpointObserver();

    static class UpdateViewpointEarthInertial implements UpdateViewpoint {
        @Override
        public Position.Q update(JHVDate time) {
            return new Position.Q(time, Sun.EpochEarthQ.distance, Sun.EpochEarthQ.orientation);
        }
    }

    static class UpdateViewpointEarth implements UpdateViewpoint {
        @Override
        public Position.Q update(JHVDate time) {
            return Sun.getEarthQuat(time);
        }
    }

    static class UpdateViewpointObserver implements UpdateViewpoint {
        @Override
        public Position.Q update(JHVDate time) {
            View view = Layers.getActiveView();
            if (view == null) {
                return Sun.getEarthQuat(time);
            } else {
                return view.getMetaData(time).getViewpoint();
            }
        }
    }

    static class UpdateViewpointExpert implements UpdateViewpoint {

        private PositionLoad positionLoad;

        void setPositionLoad(PositionLoad _positionLoad) {
            positionLoad = _positionLoad;
        }

        @Override
        public Position.Q update(JHVDate time) {
            if (positionLoad.isLoaded()) {
                long currentCameraTime;
                long tLayerStart = 0, tLayerEnd = 0;
                // Active layer times
                View view = Layers.getActiveView();
                if (view != null) {
                    tLayerStart = view.getFirstTime().milli;
                    tLayerEnd = view.getLastTime().milli;
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

}
