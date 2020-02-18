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
            return layer == null ? Sun.getEarthHCI(time) : layer.getView().getMetaData(time).getViewpoint();
        }
    }

    class Earth implements UpdateViewpoint {
        @Override
        public Position update(JHVTime time) {
            return Sun.getEarthHCI(time);
        }
    }

    class EarthFixedDistance implements UpdateViewpoint {
        @Override
        public Position update(JHVTime time) {
            return new Position(time, Sun.MeanEarthDistance, Sun.getEarthHCI(time).lon, 0);
        }
    }

    class Equatorial implements UpdateViewpoint {
        private static final double distance = 2 * Sun.MeanEarthDistance / Math.tan(0.5 * Math.PI / 180);

        @Override
        public Position update(JHVTime time) {
            JHVTime itime = time;
            List<PositionLoad> loadList = PositionLoad.get(this);
            if (!loadList.isEmpty()) {
                PositionResponse response = loadList.get(0).getResponse();
                if (response != null) {
                    itime = new JHVTime(response.interpolateTime(time.milli, Movie.getStartTime(), Movie.getEndTime()));
                }
            }

            ImageLayer layer = Layers.getActiveImageLayer();
            double lon = layer == null ? Sun.getEarthHCI(itime).lon : layer.getView().getMetaData(time).getViewpoint().lon;
            return new Position(itime, distance, lon + Math.PI / 2, Math.PI / 2);
        }
    }

    class Expert implements UpdateViewpoint {
        private final double[] lati = new double[3];

        @Override
        public Position update(JHVTime time) {
            List<PositionLoad> loadList = PositionLoad.get(this);
            if (!loadList.isEmpty()) {
                PositionResponse response = loadList.get(0).getResponse();
                if (response != null) {
                    JHVTime itime = new JHVTime(response.interpolateLatitudinal(time.milli, Movie.getStartTime(), Movie.getEndTime(), lati));
                    return new Position(itime, lati[0], lati[1], lati[2]);
                }
            }
            return Sun.getEarthHCI(time);
        }

    }

}
