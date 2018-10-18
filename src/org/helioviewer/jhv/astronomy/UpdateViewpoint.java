package org.helioviewer.jhv.astronomy;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.position.LoadPosition;
import org.helioviewer.jhv.position.Position;
import org.helioviewer.jhv.position.PositionResponse;
import org.helioviewer.jhv.time.JHVDate;
import org.helioviewer.jhv.time.TimeUtils;

public interface UpdateViewpoint {

    Position update(JHVDate time);

    void clear();

    void setLoadPosition(LoadPosition _loadPosition);

    void unsetLoadPosition(LoadPosition _loadPosition);

    Collection<LoadPosition> getLoadPositions();

    UpdateViewpoint observer = new Observer();
    UpdateViewpoint earth = new Earth();
    UpdateViewpoint earthFixedDistance = new EarthFixedDistance();
    UpdateViewpoint equatorial = new Equatorial();
    UpdateViewpoint expert = new Expert();

    abstract class AbstractUpdateViewpoint implements UpdateViewpoint {

        private final Collection<LoadPosition> loadPositions = Collections.emptySet();

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
        public Collection<LoadPosition> getLoadPositions() {
            return loadPositions;
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
        private final HashMap<SpaceObject, LoadPosition> loadMap = new HashMap<>();

        @Override
        public void clear() {
            loadMap.clear();
        }

        @Override
        public void setLoadPosition(LoadPosition loadPosition) {
            loadMap.put(loadPosition.getTarget(), loadPosition);
        }

        @Override
        public void unsetLoadPosition(LoadPosition loadPosition) {
            loadPosition.stop();
            loadMap.remove(loadPosition.getTarget());
        }

        @Override
        public Collection<LoadPosition> getLoadPositions() {
            return loadMap.values();
        }

        @Override
        public Position update(JHVDate time) {
            JHVDate itime = time;
            Iterator<LoadPosition> it = getLoadPositions().iterator();
            if (it.hasNext()) {
                PositionResponse response = it.next().getResponse();
                if (response != null) {
                    long t = response.interpolateTime(time.milli, Movie.getStartTime(), Movie.getEndTime());
                    itime = new JHVDate(TimeUtils.floorSec(t));
                }
            }

            double elon = Sun.getEarth(itime).lon;
            ImageLayer layer = Layers.getActiveImageLayer();
            double lon = layer == null ? elon : layer.getView().getMetaData(time).getViewpoint().lon;

            return new Position(itime, distance, lon + Math.PI / 2, Math.PI / 2);
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
            loadPosition.stop();
            loadPosition = null;
        }

        @Override
        public Position update(JHVDate time) {
            PositionResponse response;
            if (loadPosition == null || (response = loadPosition.getResponse()) == null)
                return Sun.getEarth(time);
            return response.getRelativeInterpolated(time.milli, Movie.getStartTime(), Movie.getEndTime());
        }

    }

}
