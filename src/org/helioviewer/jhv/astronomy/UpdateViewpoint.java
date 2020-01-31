package org.helioviewer.jhv.astronomy;

import java.util.List;

import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.time.JHVTime;

public interface UpdateViewpoint {

    Position update(JHVTime time);

    UpdateViewpoint observer = new Observer();
    UpdateViewpoint earth = new Earth();
    UpdateViewpoint earthFixedDistance = new EarthFixedDistance();
    UpdateViewpoint equatorial = new Equatorial();
    UpdateViewpoint expert = new Expert();

    class Observer implements UpdateViewpoint {
        @Override
        public Position update(JHVTime time) {
            ImageLayer layer = Layers.getActiveImageLayer();
            return layer == null ? Sun.getEarth(time) : layer.getView().getMetaData(time).getViewpoint();
        }
    }

    class Earth implements UpdateViewpoint {
        @Override
        public Position update(JHVTime time) {
            return Sun.getEarth(time);
        }
    }

    class EarthFixedDistance implements UpdateViewpoint {
        @Override
        public Position update(JHVTime time) {
            return new Position(time, Sun.MeanEarthDistance, Sun.getEarth(time).lon, 0);
        }
    }

    class Equatorial implements UpdateViewpoint {
        private static final double distance = 2 * Sun.MeanEarthDistance / Math.tan(0.5 * Math.PI / 180);

        @Override
        public Position update(JHVTime time) {
            JHVTime itime = time;
            List<PositionLoad> loadList = PositionLoad.get(this);
            if (!loadList.isEmpty()) {
                PositionLoad positionLoad = loadList.get(0);
                PositionResponse response = positionLoad.getResponse();
                if (response != null) {
                    itime = new JHVTime(response.interpolateTime(time.milli, Movie.getStartTime(), Movie.getEndTime()));
                }
            }

            ImageLayer layer = Layers.getActiveImageLayer();
            double lon = layer == null ? Sun.getEarth(itime).lon : layer.getView().getMetaData(time).getViewpoint().lon;
            return new Position(itime, distance, lon + Math.PI / 2, Math.PI / 2);
        }
    }

    class Expert implements UpdateViewpoint {
        private final double[] lat = new double[3];

        @Override
        public Position update(JHVTime time) {
            List<PositionLoad> loadList = PositionLoad.get(this);
            if (!loadList.isEmpty()) {
                PositionLoad positionLoad = loadList.get(0);
                PositionResponse response = positionLoad.getResponse();
                if (response != null) {
                    JHVTime itime = new JHVTime(response.interpolateLatitudinal(time.milli, Movie.getStartTime(), Movie.getEndTime(), lat));
                    double elon = Sun.getEarth(itime).lon;
                    return new Position(itime, lat[0], elon - lat[1], lat[2]);
                }
            }
            return Sun.getEarth(time);
        }

    }

}
