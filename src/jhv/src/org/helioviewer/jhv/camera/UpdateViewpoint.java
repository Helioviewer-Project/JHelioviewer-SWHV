package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.view.View;
import org.jetbrains.annotations.NotNull;

public interface UpdateViewpoint {

    @NotNull Position.Q update(JHVDate time);

    UpdateViewpoint updateEarthFixedDistance = new UpdateViewpointEarthFixedDistance();
    UpdateViewpoint updateEarth = new UpdateViewpointEarth();
    UpdateViewpoint updateExpert = new UpdateViewpointExpert();
    UpdateViewpoint updateObserver = new UpdateViewpointObserver();

    class UpdateViewpointEarthFixedDistance implements UpdateViewpoint {
        @NotNull
        @Override
        public Position.Q update(@NotNull JHVDate time) {
            Position.L p = Sun.getEarth(time);
            return new Position.Q(time, Sun.EpochEarthQ.distance, new Quat(0, p.lon));
        }
    }

    class UpdateViewpointEarth implements UpdateViewpoint {
        @NotNull
        @Override
        public Position.Q update(@NotNull JHVDate time) {
            return Sun.getEarthQuat(time);
        }
    }

    class UpdateViewpointObserver implements UpdateViewpoint {
        @NotNull
        @Override
        public Position.Q update(@NotNull JHVDate time) {
            View view = Layers.getActiveView();
            return view == null ? Sun.getEarthQuat(time) : view.getMetaData(time).getViewpoint();
        }
    }

    class UpdateViewpointExpert implements UpdateViewpoint {

        private PositionLoad positionLoad;

        void setPositionLoad(PositionLoad _positionLoad) {
            positionLoad = _positionLoad;
        }

        @NotNull
        @Override
        public Position.Q update(@NotNull JHVDate time) {
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
                if (tLayerEnd == tLayerStart)
                    currentCameraTime = tPositionEnd;
                else
                    currentCameraTime = (long) (tPositionStart + (tPositionEnd - tPositionStart) * (time.milli - tLayerStart) / (double) (tLayerEnd - tLayerStart));

                Position.Q pos = positionLoad.getInterpolatedPosition(currentCameraTime);
                if (pos != null)
                    return pos;
            }
            return Sun.getEarthQuat(time);
        }

    }

}
