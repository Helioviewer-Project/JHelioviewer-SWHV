package org.helioviewer.jhv.astronomy;

import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.time.JHVTime;

public interface UpdateViewpoint {

    Position update(JHVTime time);

    default Vec3 dragAxis() {
        return Vec3.YAxis;
    }

    UpdateViewpoint observer = new Observer();
    UpdateViewpoint observerAt1au = new ObserverAt1au();
    UpdateViewpoint earthAt1au = new EarthAt1au();

    class Observer implements UpdateViewpoint {
        @Override
        public Position update(JHVTime time) {
            ImageLayer layer = Layers.getActiveImageLayer();
            return layer == null ? Sun.getEarth(time) : layer.getView().getMetaData(time).getViewpoint();
        }
    }

    class ObserverAt1au implements UpdateViewpoint {
        @Override
        public Position update(JHVTime time) {
            ImageLayer layer = Layers.getActiveImageLayer();
            return layer == null
                    ? Position.toFixedDistance(Sun.getEarth(time), Sun.MeanEarthDistance)
                    : Position.toFixedDistance(layer.getView().getMetaData(time).getViewpoint(), Sun.MeanEarthDistance);
        }
    }

    class EarthAt1au implements UpdateViewpoint {
        @Override
        public Position update(JHVTime time) {
            return new Position(time, Sun.MeanEarthDistance, Sun.getEarth(time).lon, 0);
        }
    }

    final class Location implements UpdateViewpoint {
        private final PositionLoad positionLoad;
        private final long start;
        private final long end;
        private final PositionResponse.Interpolated interpolated = new PositionResponse.Interpolated();

        public Location(PositionLoad _positionLoad, long _start, long _end) {
            positionLoad = _positionLoad;
            start = _start;
            end = _end;
        }

        @Override
        public Position update(JHVTime time) {
            if (positionLoad != null) {
                PositionResponse response = positionLoad.getResponse();
                if (response != null)
                    return response.interpolateCarrington(time.milli, start, end, interpolated);
            }
            return Sun.getEarth(time);
        }
    }

    final class Equatorial implements UpdateViewpoint {
        private static final double distance = 2 * Sun.MeanEarthDistance / Math.tan(Math.toRadians(0.5));

        private final PositionLoad controlLoad;
        private final Frame frame;
        private final boolean relative;
        private final long start;
        private final long end;
        private final double[] lati = new double[3];
        private final PositionResponse.Interpolated interpolated = new PositionResponse.Interpolated();

        public Equatorial(PositionLoad _controlLoad, Frame _frame, boolean _relative, long _start, long _end) {
            controlLoad = _controlLoad;
            frame = _frame;
            relative = _relative;
            start = _start;
            end = _end;
        }

        @Override
        public Vec3 dragAxis() {
            return Vec3.ZAxis;
        }

        @Override
        public Position update(JHVTime time) {
            double hciLon = 0;
            JHVTime itime = time;

            if (controlLoad != null) {
                PositionResponse response = controlLoad.getResponse();
                if (response != null) {
                    itime = new JHVTime(response.interpolateTime(time.milli, start, end));
                    if (frame == Frame.SOLO_HCI)
                        hciLon = Sun.getEarthHCI(itime).lon;
                }
            }

            double relLon = relativeLongitude(itime.milli);
            return new Position(itime, distance, Sun.getEarth(itime).lon + hciLon - relLon + Math.PI / 2, Math.PI / 2);
        }

        private double relativeLongitude(long time) {
            if (!relative || controlLoad == null)
                return 0;

            PositionResponse response = controlLoad.getResponse();
            if (response != null) {
                response.interpolateLatitudinal(time, start, end, lati, interpolated);
                return lati[1];
            }
            return 0;
        }
    }

}
