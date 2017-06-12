package org.helioviewer.jhv.camera;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Pair;
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
        private HashSet<LoadPosition> loadSet = new HashSet<>();

        void setDistance(double d) {
            distance = d;
        }

        private Position.L getPositionInternal(LoadPosition loadPosition, JHVDate time, long layerStart, long layerEnd) {
            if (!loadPosition.isLoaded()) {
                Position.L p = Sun.getEarth(time);
                return new Position.L(time, p.rad, 0, 0);
            }
            return loadPosition.getInterpolatedL(loadPosition.interpolateTime(time.milli, layerStart, layerEnd));
        }

        public List<Pair<SpaceObject, Position.L>> getPosition(JHVDate time) {
            long layerStart = 0, layerEnd = 0;
            // Active layer times
            View view = Layers.getActiveView();
            if (view != null) {
                layerStart = view.getFirstTime().milli;
                layerEnd = view.getLastTime().milli;
            }

            ArrayList<Pair<SpaceObject, Position.L>> ret = new ArrayList<>(loadSet.size());
            for (LoadPosition loadPosition : loadSet) {
                ret.add(new Pair<>(loadPosition.getTarget(), getPositionInternal(loadPosition, time, layerStart, layerEnd)));
            }
            return ret;
        }

        @Override
        public void setLoadPosition(LoadPosition loadPosition) {
            loadSet.add(loadPosition);
        }

        @Override
        public void unsetLoadPosition(LoadPosition loadPosition) {
            loadSet.remove(loadPosition);
        }

        @Override
        public Position.Q update(JHVDate time) {
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
