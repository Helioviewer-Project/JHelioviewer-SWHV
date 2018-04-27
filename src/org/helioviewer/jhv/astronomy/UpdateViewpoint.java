package org.helioviewer.jhv.astronomy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
    Set<Map.Entry<LoadPosition, Position.L>> getPositions();

    UpdateViewpoint observer = new Observer();
    UpdateViewpoint earth = new Earth();
    UpdateViewpoint earthFixedDistance = new EarthFixedDistance();
    UpdateViewpoint equatorial = new Equatorial();
    UpdateViewpoint expert = new Expert();

    abstract class AbstractUpdateViewpoint implements UpdateViewpoint {

        private final Set<Map.Entry<LoadPosition, Position.L>> positions = Collections.emptySet();

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
        public Set<Map.Entry<LoadPosition, Position.L>> getPositions() {
            return positions;
        }

        public abstract Position.Q update(JHVDate time);

    }

    class Observer extends AbstractUpdateViewpoint {
        @Override
        public Position.Q update(JHVDate time) {
            ImageLayer layer = Layers.getActiveImageLayer();
            return layer == null ? Sun.getEarthQuat(time) : layer.getView().getMetaData(time).getViewpoint();
        }
    }

    class Earth extends AbstractUpdateViewpoint {
        @Override
        public Position.Q update(JHVDate time) {
            return Sun.getEarthQuat(time);
        }
    }

    class EarthFixedDistance extends AbstractUpdateViewpoint {
        @Override
        public Position.Q update(JHVDate time) {
            Position.L p = Sun.getEarth(time);
            return new Position.Q(time, Sun.MeanEarthDistance, new Quat(0, p.lon));
        }
    }

    class Equatorial extends AbstractUpdateViewpoint {

        private static final double distance = 2 * Sun.MeanEarthDistance / Math.tan(0.5 * Math.PI / 180);
        private final HashMap<LoadPosition, Position.L> loadMap = new HashMap<>();

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
        public Set<Map.Entry<LoadPosition, Position.L>> getPositions() {
            return loadMap.entrySet();
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

    class Expert extends AbstractUpdateViewpoint {

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
