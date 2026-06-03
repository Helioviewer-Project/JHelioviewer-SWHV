package org.helioviewer.jhv.astronomy;

import java.util.Iterator;

import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONArray;
import org.json.JSONObject;

public class PositionResponse {

    public static final class Interpolated {
        public long time;
        public double x;
        public double y;
        public double z;

        public double distance() {
            return Math.sqrt(x * x + y * y + z * z);
        }
    }

    private final Position.Cartesian[] position;
    private final long positionStart;
    private final long positionEnd;
    private final String location;

    PositionResponse(Position.Cartesian[] _position, String _location) throws Exception {
        int len = _position.length;
        if (len == 0)
            throw new Exception("Empty response");
        position = _position;
        positionStart = position[0].milli();
        positionEnd = position[len - 1].milli();
        location = _location;
    }

    PositionResponse(JSONObject jo) throws Exception {
        JSONArray res = jo.getJSONArray("result");
        int len = res.length();
        if (len == 0)
            throw new Exception("Empty response");

        position = new Position.Cartesian[len];
        for (int i = 0; i < len; i++) {
            JSONObject posObject = res.getJSONObject(i);
            Iterator<String> iterKeys = posObject.keys();
            if (!iterKeys.hasNext())
                throw new Exception("Unexpected format");

            String date = iterKeys.next();
            JSONArray posArray = posObject.getJSONArray(date);

            double x = posArray.getDouble(0) * Sun.RadiusKMeterInv;
            double y = posArray.getDouble(1) * Sun.RadiusKMeterInv;
            double z = posArray.getDouble(2) * Sun.RadiusKMeterInv;
            position[i] = new Position.Cartesian(TimeUtils.parse(date), x, y, z);
        }
        positionStart = position[0].milli();
        positionEnd = position[len - 1].milli();
        location = null;
    }

    long interpolateTime(long t, long start, long end) {
        if (start >= end)
            return positionEnd;
        else {
            double f = (t - start) / (double) (end - start); //!
            long time = (long) (positionStart + f * (positionEnd - positionStart) + .5);
            return Math.clamp(time, positionStart, positionEnd);
        }
    }

    private void interpolate(long t, long start, long end, Interpolated out) {
        long time = interpolateTime(t, start, end);
        out.time = time;

        if (positionStart == positionEnd) {
            Position.Cartesian p = position[0];
            out.x = p.x();
            out.y = p.y();
            out.z = p.z();
            return;
        }

        int maxIndex = position.length - 1;
        double interpolatedIndex = (time - positionStart) / (double) (positionEnd - positionStart) * maxIndex;
        int i = Math.clamp((int) interpolatedIndex, 0, maxIndex);
        int inext = Math.min(i + 1, maxIndex);

        long tstart = position[i].milli();
        long tend = position[inext].milli();

        double alpha = tend == tstart ? 1. : Math.clamp((time - tstart) / (double) (tend - tstart), 0, 1);

        out.x = (1. - alpha) * position[i].x() + alpha * position[inext].x();
        out.y = (1. - alpha) * position[i].y() + alpha * position[inext].y();
        out.z = (1. - alpha) * position[i].z() + alpha * position[inext].z();
    }

    public double interpolateRectangular(long t, long start, long end, float[] xyz, Interpolated out) {
        interpolate(t, start, end, out);

        xyz[0] = (float) out.x;
        xyz[1] = (float) out.y;
        xyz[2] = (float) out.z;
        return out.distance();
    }

    public double interpolateRectangular(long t, long start, long end, double[] xyz, Interpolated out) {
        interpolate(t, start, end, out);

        xyz[0] = out.x;
        xyz[1] = out.y;
        xyz[2] = out.z;
        return out.distance();
    }

    public void interpolateLatitudinal(long t, long start, long end, double[] lati, Interpolated out) {
        interpolate(t, start, end, out);
        double dist, hgln, hglt;
        dist = out.distance();
        if (dist == 0) {
            hgln = 0;
            hglt = 0;
        } else {
            hgln = Math.atan2(out.y, out.x);
            double sinLat = Math.clamp(out.z / dist, -1., 1.);
            hglt = Math.asin(sinLat);
        }
        lati[0] = dist;
        lati[1] = hgln;
        lati[2] = hglt;
    }

    Position interpolateCarrington(long t, long start, long end, Interpolated out) {
        interpolate(t, start, end, out);
        double dist, hgln, hglt;
        dist = out.distance();
        if (dist == 0) {
            hgln = 0;
            hglt = 0;
        } else {
            hgln = Math.atan2(out.y, out.x);
            if (hgln < 0)
                hgln += 2 * Math.PI;
            double sinLat = Math.clamp(out.z / dist, -1., 1.);
            hglt = Math.asin(sinLat);
        }
        return new Position(new JHVTime(out.time), dist, -hgln, hglt).setLocation(location);
    }

}
