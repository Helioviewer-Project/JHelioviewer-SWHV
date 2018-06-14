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

public interface UpdateViewpoint {

    Position update(JHVDate time);
    void clear();
    void setTime(long _start, long _end);
    long interpolateTime(long time);
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
        private long start;
        private long end;

        @Override
        public void clear() {
        }

        @Override
        public void setTime(long _start, long _end) {
            start = _start;
            end = _end;
        }

        @Override
        public long interpolateTime(long time) {
            long mstart = Movie.getStartTime();
            long mend = Movie.getEndTime();
            if (mstart == mend)
                return end;

            double f = (time - mstart) / (double) (mend - mstart);
            return (long) (start + f * (end - start) + .5);
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
            long t = interpolateTime(time.milli);
            for (LoadPosition loadPosition : loadMap.keySet()) {
                if (loadPosition.isLoaded())
                    loadMap.put(loadPosition, loadPosition.getInterpolated(t));
            }

            JHVDate itime = new JHVDate(t);
            double elon = Sun.getEarth(itime).lon;
            return new Position(itime, distance, elon, Math.PI / 2);
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
            return loadPosition.getRelativeInterpolated(interpolateTime(time.milli));
        }

    }

}
