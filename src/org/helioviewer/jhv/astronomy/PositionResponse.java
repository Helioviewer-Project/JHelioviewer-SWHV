package org.helioviewer.jhv.astronomy;

import java.util.Iterator;

import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class PositionResponse {

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
            return MathUtils.clip(time, positionStart, positionEnd);
        }
    }

    public void interpolateLatitudinal(long t, long start, long end, double[] lati) {
        long time = interpolateTime(t, start, end);

        double x, y, z;
        if (positionStart == positionEnd) {
            x = position[0].x();
            y = position[0].y();
            z = position[0].z();
        } else {
            int maxIndex = position.length - 1;
            double interpolatedIndex = (time - positionStart) / (double) (positionEnd - positionStart) * maxIndex;
            int i = (int) interpolatedIndex;
            i = MathUtils.clip(i, 0, maxIndex);
            int inext = Math.min(i + 1, maxIndex);

            long tstart = position[i].milli();
            long tend = position[inext].milli();

            double alpha = tend == tstart ? 1. : MathUtils.clip((time - tstart) / (double) (tend - tstart), 0, 1);
            x = (1. - alpha) * position[i].x() + alpha * position[inext].x();
            y = (1. - alpha) * position[i].y() + alpha * position[inext].y();
            z = (1. - alpha) * position[i].z() + alpha * position[inext].z();
        }

        double dist, hgln, hglt;
        dist = Math.sqrt(x * x + y * y + z * z);
        if (dist == 0) {
            hgln = 0;
            hglt = 0;
        } else {
            hgln = Math.atan2(y, x);
            hglt = Math.asin(z / dist);
        }
        lati[0] = dist;
        lati[1] = hgln;
        lati[2] = hglt;
    }

    Position interpolateCarrington(long t, long start, long end) {
        long time = interpolateTime(t, start, end);

        double x, y, z;
        if (positionStart == positionEnd) {
            x = position[0].x();
            y = position[0].y();
            z = position[0].z();
        } else {
            int maxIndex = position.length - 1;
            double interpolatedIndex = (time - positionStart) / (double) (positionEnd - positionStart) * maxIndex;
            int i = (int) interpolatedIndex;
            i = MathUtils.clip(i, 0, maxIndex);
            int inext = Math.min(i + 1, maxIndex);

            long tstart = position[i].milli();
            long tend = position[inext].milli();

            double alpha = tend == tstart ? 1. : MathUtils.clip((time - tstart) / (double) (tend - tstart), 0, 1);
            x = (1. - alpha) * position[i].x() + alpha * position[inext].x();
            y = (1. - alpha) * position[i].y() + alpha * position[inext].y();
            z = (1. - alpha) * position[i].z() + alpha * position[inext].z();
        }

        double dist, hgln, hglt;
        dist = Math.sqrt(x * x + y * y + z * z);
        if (dist == 0) {
            hgln = 0;
            hglt = 0;
        } else {
            hgln = Math.atan2(y, x);
            if (hgln < 0)
                hgln += 2 * Math.PI;
            hglt = Math.asin(z / dist);
        }
        return new Position(new JHVTime(time), dist, -hgln, hglt).setLocation(location);
    }

    public double interpolateRectangular(long t, long start, long end, float[] xyz) {
        long time = interpolateTime(t, start, end);

        double x, y, z;
        if (positionStart == positionEnd) {
            x = position[0].x();
            y = position[0].y();
            z = position[0].z();
        } else {
            int maxIndex = position.length - 1;
            double interpolatedIndex = (time - positionStart) / (double) (positionEnd - positionStart) * maxIndex;
            int i = (int) interpolatedIndex;
            i = MathUtils.clip(i, 0, maxIndex);
            int inext = Math.min(i + 1, maxIndex);

            long tstart = position[i].milli();
            long tend = position[inext].milli();

            double alpha = tend == tstart ? 1. : MathUtils.clip((time - tstart) / (double) (tend - tstart), 0, 1);
            x = (1. - alpha) * position[i].x() + alpha * position[inext].x();
            y = (1. - alpha) * position[i].y() + alpha * position[inext].y();
            z = (1. - alpha) * position[i].z() + alpha * position[inext].z();
        }
        xyz[0] = (float) x;
        xyz[1] = (float) y;
        xyz[2] = (float) z;

        return Math.sqrt(x * x + y * y + z * z);
    }

}
