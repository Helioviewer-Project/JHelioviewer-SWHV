package org.helioviewer.jhv.camera.viewpoint;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.PositionLoad;
import org.helioviewer.jhv.astronomy.PositionResponse;
import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeUtils;

public interface UpdateViewpoint {

    Position update(JHVTime time);

    void clear();

    void setPositionLoad(PositionLoad _positionLoad);

    void unsetPositionLoad(PositionLoad _positionLoad);

    Collection<PositionLoad> getPositionLoads();

    UpdateViewpoint observer = new Observer();
    UpdateViewpoint earth = new Earth();
    UpdateViewpoint earthFixedDistance = new EarthFixedDistance();
    UpdateViewpoint equatorial = new Equatorial();
    UpdateViewpoint expert = new Expert();

    abstract class AbstractUpdateViewpoint implements UpdateViewpoint {

        private final Collection<PositionLoad> positionLoads = Collections.emptySet();

        @Override
        public void clear() {
        }

        @Override
        public void setPositionLoad(PositionLoad _positionLoad) {
        }

        @Override
        public void unsetPositionLoad(PositionLoad _positionLoad) {
        }

        @Override
        public Collection<PositionLoad> getPositionLoads() {
            return positionLoads;
        }

        @Override
        public abstract Position update(JHVTime time);

    }

    class Observer extends AbstractUpdateViewpoint {
        @Override
        public Position update(JHVTime time) {
            ImageLayer layer = Layers.getActiveImageLayer();
            return layer == null ? Sun.getEarth(time) : layer.getView().getMetaData(time).getViewpoint();
        }
    }

    class Earth extends AbstractUpdateViewpoint {
        @Override
        public Position update(JHVTime time) {
            return Sun.getEarth(time);
        }
    }

    class EarthFixedDistance extends AbstractUpdateViewpoint {
        @Override
        public Position update(JHVTime time) {
            return new Position(time, Sun.MeanEarthDistance, Sun.getEarth(time).lon, 0);
        }
    }

    class Equatorial extends AbstractUpdateViewpoint {

        private static final double distance = 2 * Sun.MeanEarthDistance / Math.tan(0.5 * Math.PI / 180);
        private final HashMap<SpaceObject, PositionLoad> loadMap = new HashMap<>();

        @Override
        public void clear() {
            loadMap.clear();
        }

        @Override
        public void setPositionLoad(PositionLoad positionLoad) {
            loadMap.put(positionLoad.getTarget(), positionLoad);
        }

        @Override
        public void unsetPositionLoad(PositionLoad positionLoad) {
            positionLoad.stop();
            loadMap.remove(positionLoad.getTarget());
        }

        @Override
        public Collection<PositionLoad> getPositionLoads() {
            return loadMap.values();
        }

        @Override
        public Position update(JHVTime time) {
            JHVTime itime = time;
            Iterator<PositionLoad> it = getPositionLoads().iterator();
            if (it.hasNext()) {
                PositionResponse response = it.next().getResponse();
                if (response != null) {
                    long t = response.interpolateTime(time.milli, Movie.getStartTime(), Movie.getEndTime());
                    itime = new JHVTime(TimeUtils.floorSec(t));
                }
            }

            ImageLayer layer = Layers.getActiveImageLayer();
            double lon = layer == null ? Sun.getEarth(itime).lon : layer.getView().getMetaData(time).getViewpoint().lon;
            return new Position(itime, distance, lon + Math.PI / 2, Math.PI / 2);
        }
    }

    class Expert extends AbstractUpdateViewpoint {

        private PositionLoad positionLoad;

        @Override
        public void clear() {
            positionLoad = null;
        }

        @Override
        public void setPositionLoad(PositionLoad _positionLoad) {
            positionLoad = _positionLoad;
        }

        @Override
        public void unsetPositionLoad(PositionLoad _positionLoad) {
            positionLoad.stop();
            positionLoad = null;
        }

        @Override
        public Position update(JHVTime time) {
            PositionResponse response;
            if (positionLoad == null || (response = positionLoad.getResponse()) == null)
                return Sun.getEarth(time);
            return response.getRelativeInterpolated(time.milli, Movie.getStartTime(), Movie.getEndTime());
        }

    }

}
