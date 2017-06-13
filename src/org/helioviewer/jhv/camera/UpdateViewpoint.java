package org.helioviewer.jhv.camera;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.time.JHVDate;
import org.helioviewer.jhv.view.View;

public interface UpdateViewpoint {

    Position.Q update(JHVDate time);
    void setLoadPosition(LoadPosition _loadPosition);
    void unsetLoadPosition(LoadPosition _loadPosition);

    Observer observer = new Observer();
    Earth earth = new Earth();
    EarthFixedDistance earthFixedDistance = new EarthFixedDistance();
    Ecliptic ecliptic = new Ecliptic();
    Expert expert = new Expert();

    class Observer implements UpdateViewpoint {
        @Override
        public void setLoadPosition(LoadPosition _loadPosition) {
        }

        @Override
        public void unsetLoadPosition(LoadPosition _loadPosition) {
        }

        @Override
        public Position.Q update(JHVDate time) {
            View view = Layers.getActiveView();
            return view == null ? Sun.getEarthQuat(time) : view.getMetaData(time).getViewpoint();
        }
    }

    class Earth implements UpdateViewpoint {
        @Override
        public void setLoadPosition(LoadPosition _loadPosition) {
        }

        @Override
        public void unsetLoadPosition(LoadPosition _loadPosition) {
        }

        @Override
        public Position.Q update(JHVDate time) {
            return Sun.getEarthQuat(time);
        }
    }

    class EarthFixedDistance implements UpdateViewpoint {
        @Override
        public void setLoadPosition(LoadPosition _loadPosition) {
        }

        @Override
        public void unsetLoadPosition(LoadPosition _loadPosition) {
        }

        @Override
        public Position.Q update(JHVDate time) {
            Position.L p = Sun.getEarth(time);
            return new Position.Q(time, Sun.MeanEarthDistance, new Quat(0, p.lon));
        }
    }

    class Ecliptic implements UpdateViewpoint {

        private double distance;
        private HashMap<LoadPosition, Position.L> loadMap = new HashMap<>();

        void setDistance(double d) {
            distance = d;
        }

        public Set<Map.Entry<LoadPosition, Position.L>> getPositions() {
            return loadMap.entrySet();
        }

        @Override
        public void setLoadPosition(LoadPosition loadPosition) {
            loadMap.put(loadPosition, null);
        }

        @Override
        public void unsetLoadPosition(LoadPosition loadPosition) {
            loadMap.remove(loadPosition);
        }

        @Override
        public Position.Q update(JHVDate time) {
            long layerStart = 0, layerEnd = 0;
            // Active layer times
            View view = Layers.getActiveView();
            if (view != null) {
                layerStart = view.getFirstTime().milli;
                layerEnd = view.getLastTime().milli;
            }

            for (LoadPosition loadPosition : loadMap.keySet()) {
                if (!loadPosition.isLoaded()) {
                    Position.L p = Sun.getEarth(time);
                    loadMap.put(loadPosition, new Position.L(time, p.rad, 0, 0));
                    continue;
                }
                loadMap.put(loadPosition, loadPosition.getInterpolatedL(loadPosition.interpolateTime(time.milli, layerStart, layerEnd)));
            }

            return new Position.Q(time, distance, Quat.rotate(Quat.Q90, Sun.getEarthQuat(time).orientation));
        }
    }

    class Expert implements UpdateViewpoint {

        private LoadPosition loadPosition;

        @Override
        public void setLoadPosition(LoadPosition _loadPosition) {
            loadPosition = _loadPosition;
        }

        @Override
        public void unsetLoadPosition(LoadPosition _loadPosition) {
            loadPosition = null;
        }

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
