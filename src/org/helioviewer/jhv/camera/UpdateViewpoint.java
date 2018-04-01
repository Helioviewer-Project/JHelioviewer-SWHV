package org.helioviewer.jhv.camera;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.io.LoadPosition;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.time.JHVDate;
import org.helioviewer.jhv.view.View;

public interface UpdateViewpoint {

    Position.Q update(JHVDate time);
    void clear();
    void setLoadPosition(LoadPosition _loadPosition);
    void unsetLoadPosition(LoadPosition _loadPosition);

    Observer observer = new Observer();
    Earth earth = new Earth();
    EarthFixedDistance earthFixedDistance = new EarthFixedDistance();
    Equatorial equatorial = new Equatorial();
    Expert expert = new Expert();

    class Observer implements UpdateViewpoint {
        @Override
        public void clear() {
        }

        @Override
        public void setLoadPosition(LoadPosition _loadPosition) {
        }

        @Override
        public void unsetLoadPosition(LoadPosition _loadPosition) {
        }

        @Override
        public Position.Q update(JHVDate time) {
            ImageLayer layer = Layers.getActiveImageLayer();
            return layer == null ? Sun.getEarthQuat(time) : layer.getView().getMetaData(time).getViewpoint();
        }
    }

    class Earth implements UpdateViewpoint {
        @Override
        public void clear() {
        }

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
        public void clear() {
        }

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

    class Equatorial implements UpdateViewpoint {

        private static final double distance = 2 * Sun.MeanEarthDistance / Math.tan(0.5 * Math.PI / 180);
        private final HashMap<LoadPosition, Position.L> loadMap = new HashMap<>();

        public Set<Map.Entry<LoadPosition, Position.L>> getPositions() {
            return loadMap.entrySet();
        }

        @Override
        public void clear() {
            loadMap.clear();
        }

        @Override
        public void setLoadPosition(LoadPosition loadPosition) {
            Position.L p = Sun.getEarth(Movie.getTime());
            loadMap.put(loadPosition, new Position.L(p.time, p.rad, 0, /* -? */ p.lat));
        }

        @Override
        public void unsetLoadPosition(LoadPosition loadPosition) {
            loadMap.remove(loadPosition);
        }

        @Override
        public Position.Q update(JHVDate time) {
            long layerStart = 0, layerEnd = 0;
            // Active layer times
            ImageLayer layer = Layers.getActiveImageLayer();
            if (layer != null) {
                View view = layer.getView();
                layerStart = view.getFirstTime().milli;
                layerEnd = view.getLastTime().milli;
            }

            for (LoadPosition loadPosition : loadMap.keySet()) {
                if (loadPosition.isLoaded())
                    loadMap.put(loadPosition, loadPosition.getInterpolatedL(time.milli, layerStart, layerEnd));
            }

            Position.L p = Sun.getEarth(time);
            return new Position.Q(time, distance, new Quat(Math.PI / 2, p.lon));
        }
    }

    class Expert implements UpdateViewpoint {

        private LoadPosition loadPosition;

        @Override
        public void clear() {
            loadPosition = null;
        }

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
            ImageLayer layer = Layers.getActiveImageLayer();
            if (layer != null) {
                View view = layer.getView();
                layerStart = view.getFirstTime().milli;
                layerEnd = view.getLastTime().milli;
            }
            return loadPosition.getInterpolatedQ(time.milli, layerStart, layerEnd);
        }

    }

}
