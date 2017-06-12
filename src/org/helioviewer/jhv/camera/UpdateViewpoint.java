package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.time.JHVDate;
import org.helioviewer.jhv.view.View;

public interface UpdateViewpoint {

    Position.Q update(JHVDate time);
    void setLoadPosition(LoadPosition _loadPosition);

    Observer observer = new Observer();
    Earth earth = new Earth();
    EarthFixedDistance earthFixedDistance = new EarthFixedDistance();
    Ecliptic ecliptic = new Ecliptic();
    Expert expert = new Expert();

    class Observer extends Viewpoint {
        @Override
        public Position.Q update(JHVDate time) {
            View view = Layers.getActiveView();
            return view == null ? Sun.getEarthQuat(time) : view.getMetaData(time).getViewpoint();
        }
    }

    class Earth extends Viewpoint {
        @Override
        public Position.Q update(JHVDate time) {
            return Sun.getEarthQuat(time);
        }
    }

    class EarthFixedDistance extends Viewpoint {
        @Override
        public Position.Q update(JHVDate time) {
            Position.L p = Sun.getEarth(time);
            return new Position.Q(time, Sun.MeanEarthDistance, new Quat(0, p.lon));
        }
    }

    class Ecliptic extends Viewpoint {

        private double distance;

        void setDistance(double d) {
            distance = d;
        }

        public Position.L getPosition(JHVDate time) {
            if (loadPosition == null || !loadPosition.isLoaded()) {
                Position.L p = Sun.getEarth(time);
                return new Position.L(time, p.rad, 0, 0);
            }

            long layerStart = 0, layerEnd = 0;
            // Active layer times
            View view = Layers.getActiveView();
            if (view != null) {
                layerStart = view.getFirstTime().milli;
                layerEnd = view.getLastTime().milli;
            }
            return loadPosition.getInterpolatedL(loadPosition.interpolateTime(time.milli, layerStart, layerEnd));
        }

        @Override
        public Position.Q update(JHVDate time) {
            return new Position.Q(time, distance, Quat.rotate(Quat.Q90, Sun.getEarthQuat(time).orientation));
        }
    }

    class Expert extends Viewpoint {
        @Override
        public Position.Q update(JHVDate time) {
            if (loadPosition == null || !loadPosition.isLoaded())
                return Sun.getEarthQuat(time);

            long layerStart = 0, layerEnd = 0;
            // Active layer times
            View view = Layers.getActiveView();
            if (view != null) {
                layerStart = view.getFirstTime().milli;
                layerEnd = view.getLastTime().milli;
            }
            return loadPosition.getInterpolatedQ(loadPosition.interpolateTime(time.milli, layerStart, layerEnd));
        }

    }

}
