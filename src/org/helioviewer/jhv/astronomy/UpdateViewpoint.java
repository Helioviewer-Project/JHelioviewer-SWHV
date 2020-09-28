package org.helioviewer.jhv.astronomy;

import java.util.List;

import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.time.JHVTime;

public interface UpdateViewpoint {

    Position update(JHVTime time);

    UpdateViewpoint observer = new Observer();
    UpdateViewpoint earthFixedDistance = new EarthFixedDistance();
    UpdateViewpoint equatorial = new Equatorial();
    UpdateViewpoint location = new Location();

    class Observer implements UpdateViewpoint {
        @Override
        public Position update(JHVTime time) {
            ImageLayer layer = Layers.getActiveImageLayer();
            return layer == null ? Sun.getEarth(time) : layer.getView().getMetaData(time).getViewpoint();
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
            double hciLon = 0;
            JHVTime itime = time;
            List<PositionLoad> loadList = PositionLoad.get(this);
            if (!loadList.isEmpty()) {
                PositionLoad load = loadList.get(0);
                PositionResponse response = load.getResponse();
                if (response != null) {
                    itime = new JHVTime(response.interpolateTime(time.milli, Movie.getStartTime(), Movie.getEndTime()));
                    if (load.isHCI())
                        hciLon = Sun.getEarthHCI(itime).lon;
                }
            }

            double relLon = Layers.getViewpointLayer().getRelativeLongitude();
            return new Position(itime, distance, Sun.getEarth(itime).lon + hciLon - relLon + Math.PI / 2, Math.PI / 2);
        }
    }

    class Location implements UpdateViewpoint {
        @Override
        public Position update(JHVTime time) {
            List<PositionLoad> loadList = PositionLoad.get(this);
            if (!loadList.isEmpty()) {
                PositionResponse response = loadList.get(0).getResponse();
                if (response != null) {
                    return response.interpolateCarrington(time.milli, Movie.getStartTime(), Movie.getEndTime());
                }
            }
            return Sun.getEarth(time);
        }

    }

}
