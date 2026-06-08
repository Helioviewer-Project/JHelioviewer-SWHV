package org.helioviewer.jhv.layers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.helioviewer.jhv.astronomy.PositionLoad;
import org.helioviewer.jhv.astronomy.PositionResponse;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.opengl.BufVertex;

final class ViewpointOrbitTrail {

    private static final double DELTA_ORBIT = 2 * 60 * 1000 * Sun.MeanEarthDistanceInv;
    private static final double DELTA_CUTOFF = 3 * Sun.MeanEarthDistance;

    static final class Cache {
        private final Map<PositionLoad, ViewpointOrbitTrail> trails = new HashMap<>();

        void prune(Collection<PositionLoad> positionLoads) {
            trails.keySet().removeIf(positionLoad -> !positionLoads.contains(positionLoad));
        }

        void clear() {
            trails.clear();
        }

        ViewpointOrbitTrail get(PositionLoad positionLoad, PositionResponse response, long start, long end) {
            ViewpointOrbitTrail trail = trails.get(positionLoad);
            if (trail == null || !trail.matches(response, start, end)) {
                trail = new ViewpointOrbitTrail(response, start, end);
                trails.put(positionLoad, trail);
            }
            return trail;
        }
    }

    private record Point(long time, float x, float y, float z, double dist) {
        Point(long _time, float[] xyzw, double _dist) {
            this(_time, xyzw[0], xyzw[1], xyzw[2], _dist);
        }

        void put(float[] xyzw) {
            xyzw[0] = x;
            xyzw[1] = y;
            xyzw[2] = z;
            xyzw[3] = 1;
        }
    }

    private final PositionResponse response;
    private final long start;
    private final long end;
    private final ArrayList<Point> points = new ArrayList<>();
    private final float[] xyz = {0, 0, 0, 1};
    private final PositionResponse.Interpolated interpolated = new PositionResponse.Interpolated();

    ViewpointOrbitTrail(PositionResponse _response, long _start, long _end) {
        response = _response;
        start = _start;
        end = _end;
        addPoint(start);
    }

    boolean matches(PositionResponse _response, long _start, long _end) {
        return response == _response && start == _start && end == _end;
    }

    void putVertices(BufVertex orbitBuf, float[] currentPoint, byte[] color, long time) {
        sampleThrough(time);
        int count = upperBound(time);

        Point first = points.getFirst();
        orbitBuf.putVertex(first.x, first.y, first.z, 1, Colors.Null);
        orbitBuf.repeatVertex(color);
        for (int i = 1; i < count; i++) {
            Point point = points.get(i);
            orbitBuf.putVertex(point.x, point.y, point.z, 1, color);
        }

        Point last = points.get(count - 1);
        if (last.time == time) {
            last.put(currentPoint);
        } else {
            response.interpolateRectangular(time, start, end, currentPoint, interpolated);
            orbitBuf.putVertex(currentPoint[0], currentPoint[1], currentPoint[2], currentPoint[3], color);
        }
        orbitBuf.repeatVertex(Colors.Null);
    }

    private void sampleThrough(long time) {
        while (true) {
            Point last = points.getLast();
            if (last.time >= time)
                return;

            long delta = getStep(last.dist);
            if (delta <= 0)
                return;

            long next = last.time + delta;
            if (next > time)
                return;

            addPoint(next);
        }
    }

    private int upperBound(long time) {
        int low = 0;
        int high = points.size();
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (points.get(mid).time <= time)
                low = mid + 1;
            else
                high = mid;
        }
        return low;
    }

    private void addPoint(long t) {
        double dist = response.interpolateRectangular(t, start, end, xyz, interpolated);
        points.add(new Point(t, xyz, dist));
    }

    private static long getStep(double dist) { // decrease interpolation step proportionally with distance, stop at 3au
        return (long) (DELTA_ORBIT * Math.min(dist, DELTA_CUTOFF));
    }
}
