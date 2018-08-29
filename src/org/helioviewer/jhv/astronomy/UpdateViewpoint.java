package org.helioviewer.jhv.astronomy;

import java.util.Collection;
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

    void addLoader(LoadPosition _loadPosition);

    void removeLoader(LoadPosition _loadPosition);

    void setObserver(LoadPosition _loadPosition);

    Collection<LoadPosition> getLoadPositions();

    UpdateViewpoint observer = new Observer();
    UpdateViewpoint earth = new Earth();
    UpdateViewpoint earthFixedDistance = new EarthFixedDistance();
    UpdateViewpoint equatorial = new Equatorial();
    UpdateViewpoint expert = new Expert();

    abstract class AbstractUpdateViewpoint implements UpdateViewpoint {

        protected final HashMap<SpaceObject, LoadPosition> loadMap = new HashMap<>();

        @Override
        public void clear() {
            for (LoadPosition load : loadMap.values())
                load.stop();
            loadMap.clear();
        }

        @Override
        public void addLoader(LoadPosition loadPosition) {
            loadMap.put(loadPosition.getTarget(), loadPosition);
        }

        @Override
        public void removeLoader(LoadPosition loadPosition) {
            loadPosition.stop();
            loadMap.remove(loadPosition.getTarget());
        }

        @Override
        public void setObserver(LoadPosition _loadPosition) {
        }

        @Override
        public Collection<LoadPosition> getLoadPositions() {
            return loadMap.values();
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
            return new Position(itime, distance, elon + Math.PI / 2, Math.PI / 2);
        }
    }

    class Expert extends AbstractUpdateViewpoint {

        private LoadPosition observerLoader;

        @Override
        public void clear() {
            super.clear();
            unsetObserver();
        }

        @Override
        public void setObserver(LoadPosition _observerLoader) {
            unsetObserver();
            observerLoader = _observerLoader;
        }

        private void unsetObserver() {
            if (observerLoader != null) {
                observerLoader.stop();
                observerLoader = null;
            }
        }

        @Override
        public Position update(JHVDate time) {
            PositionResponse response;
            if (observerLoader == null || (response = observerLoader.getResponse()) == null)
                return Sun.getEarth(time);
            return response.getRelativeInterpolated(time.milli, Movie.getStartTime(), Movie.getEndTime());
        }

    }

}
