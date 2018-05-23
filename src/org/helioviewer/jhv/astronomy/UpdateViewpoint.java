package org.helioviewer.jhv.astronomy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.helioviewer.jhv.io.LoadPosition;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.time.JHVDate;
import org.helioviewer.jhv.view.View;

public interface UpdateViewpoint {

    Position update(JHVDate time);
    void clear();
    void setLoadPosition(LoadPosition _loadPosition);
    void unsetLoadPosition(LoadPosition _loadPosition);
    Set<Map.Entry<LoadPosition, Position>> getPositions();

    UpdateViewpoint observer = new Observer();
    UpdateViewpoint earth = new Earth();
    UpdateViewpoint earthFixedDistance = new EarthFixedDistance();
    UpdateViewpoint equatorial = new Equatorial();
    UpdateViewpoint expert = new Expert();

    abstract class AbstractUpdateViewpoint implements UpdateViewpoint {

        private final Set<Map.Entry<LoadPosition, Position>> positions = Collections.emptySet();

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
        public Set<Map.Entry<LoadPosition, Position>> getPositions() {
            return positions;
        }

        @Override
        public abstract Position update(JHVDate time);

    }

    class Observer extends AbstractUpdateViewpoint {
        @Override
        public Position update(JHVDate time) {
            ImageLayer layer = Layers.getActiveImageLayer();
            return layer == null ? Sun.getEarth(time) : layer.getView().getMetaData(time).getViewpoint();
        }
    }

    class Earth extends AbstractUpdateViewpoint {
        @Override
        public Position update(JHVDate time) {
            return Sun.getEarth(time);
        }
    }

    class EarthFixedDistance extends AbstractUpdateViewpoint {
        @Override
        public Position update(JHVDate time) {
            double elon = Sun.getEarth(time).lon;
            return new Position(time, Sun.MeanEarthDistance, elon, 0);
        }
    }

    class Equatorial extends AbstractUpdateViewpoint {

        private static final double distance = 2 * Sun.MeanEarthDistance / Math.tan(0.5 * Math.PI / 180);
        private final HashMap<LoadPosition, Position> loadMap = new HashMap<>();

        @Override
        public void clear() {
            loadMap.clear();
        }

        @Override
        public void setLoadPosition(LoadPosition loadPosition) {
            Position p = Sun.getEarth(Movie.getTime());
            loadMap.put(loadPosition, new Position(p.time, p.distance, 0, /* -? */ p.lat));
        }

        @Override
        public void unsetLoadPosition(LoadPosition loadPosition) {
            loadMap.remove(loadPosition);
        }

        @Override
        public Set<Map.Entry<LoadPosition, Position>> getPositions() {
            return loadMap.entrySet();
        }

        @Override
        public Position update(JHVDate time) {
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
                    loadMap.put(loadPosition, loadPosition.getInterpolated(time.milli, layerStart, layerEnd));
            }

            double elon = Sun.getEarth(time).lon;
            return new Position(time, distance, elon, Math.PI / 2);
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
        public Position update(JHVDate time) {
            if (loadPosition == null || !loadPosition.isLoaded())
                return Sun.getEarth(time);

            long layerStart = 0, layerEnd = 0;
            // Active layer times
            ImageLayer layer = Layers.getActiveImageLayer();
            if (layer != null) {
                View view = layer.getView();
                layerStart = view.getFirstTime().milli;
                layerEnd = view.getLastTime().milli;
            }
            return loadPosition.getRelativeInterpolated(time.milli, layerStart, layerEnd);
        }

    }

}
