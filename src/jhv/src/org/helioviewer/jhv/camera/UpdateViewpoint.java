package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.view.View;

public interface UpdateViewpoint {

    Position.Q update(JHVDate time);

    UpdateViewpoint updateEarthFixedDistance = new UpdateViewpointEarthFixedDistance();
    UpdateViewpoint updateEarth = new UpdateViewpointEarth();
    UpdateViewpoint updateExpert = new UpdateViewpointExpert();
    UpdateViewpoint updateObserver = new UpdateViewpointObserver();

    class UpdateViewpointEarthFixedDistance implements UpdateViewpoint {
        @Override
        public Position.Q update(JHVDate time) {
            Position.L p = Sun.getEarth(time);
            return new Position.Q(time, Sun.EpochEarthQ.distance, new Quat(0, p.lon));
        }
    }

    class UpdateViewpointEarth implements UpdateViewpoint {
        @Override
        public Position.Q update(JHVDate time) {
            return Sun.getEarthQuat(time);
        }
    }

    class UpdateViewpointObserver implements UpdateViewpoint {
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

    class UpdateViewpointExpert implements UpdateViewpoint {

        private PositionLoad positionLoad;

        void setPositionLoad(PositionLoad _positionLoad) {
            positionLoad = _positionLoad;
        }

        @Override
        public Position.Q update(JHVDate time) {
            if (positionLoad.isLoaded()) {
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

                long currentCameraTime;
                if (tLayerEnd != tLayerStart) {
                    currentCameraTime = (long) (tPositionStart + (tPositionEnd - tPositionStart) * (time.milli - tLayerStart) / (double) (tLayerEnd - tLayerStart));
                } else {
                    currentCameraTime = tPositionEnd;
                }
                Position.Q pos = positionLoad.getInterpolatedPosition(currentCameraTime);
                if (pos != null)
                    return pos;
            }
            return Sun.getEarthQuat(time);
        }

    }

}
