package org.helioviewer.jhv.astronomy;

import java.util.Iterator;

import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.time.JHVDate;
import org.helioviewer.jhv.time.TimeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class PositionResponse {

    private final PositionCartesian[] position;
    private final long positionStart;
    private final long positionEnd;

    PositionResponse(PositionCartesian[] _position) throws Exception {
        int len = _position.length;
        if (len == 0)
            throw new Exception("Empty response");
        position = _position;
        positionStart = position[0].milli;
        positionEnd = position[len - 1].milli;
    }

    PositionResponse(JSONObject jo) throws Exception {
        JSONArray res = jo.getJSONArray("result");
        int len = res.length();
        if (len == 0)
            throw new Exception("Empty response");

        position = new PositionCartesian[len];
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
            position[i] = new PositionCartesian(TimeUtils.parse(date), x, y, z);
        }
        positionStart = position[0].milli;
        positionEnd = position[len - 1].milli;
    }

    public long interpolateTime(long t, long start, long end) {
        if (start >= end)
            return positionEnd;
        else {
            double f = (t - start) / (double) (end - start); //!
            long time = (long) (positionStart + f * (positionEnd - positionStart) + .5);
            return MathUtils.clip(time, positionStart, positionEnd);
        }
    }

    public Position getRelativeInterpolated(long t, long start, long end) {
        long time = interpolateTime(t, start, end);

        double x, y, z;
        if (positionStart == positionEnd) {
            x = position[0].x;
            y = position[0].y;
            z = position[0].z;
        } else {
            double interpolatedIndex = (time - positionStart) / (double) (positionEnd - positionStart) * position.length;
            int i = (int) interpolatedIndex;
            i = MathUtils.clip(i, 0, position.length - 1);
            int inext = Math.min(i + 1, position.length - 1);

            long tstart = position[i].milli;
            long tend = position[inext].milli;

            double alpha = tend == tstart ? 1. : ((time - tstart) / (double) (tend - tstart)) % 1.;
            x = (1. - alpha) * position[i].x + alpha * position[inext].x;
            y = (1. - alpha) * position[i].y + alpha * position[inext].y;
            z = (1. - alpha) * position[i].z + alpha * position[inext].z;
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

        JHVDate date = new JHVDate(time);
        double elon = Sun.getEarth(date).lon;
        return new Position(date, dist, elon - hgln, hglt);
    }

    public Vec3 getInterpolatedHG(long t, long start, long end) {
        long time = interpolateTime(t, start, end);

        double x, y, z;
        if (positionStart == positionEnd) {
            x = position[0].x;
            y = position[0].y;
            z = position[0].z;
        } else {
            double interpolatedIndex = (time - positionStart) / (double) (positionEnd - positionStart) * position.length;
            int i = (int) interpolatedIndex;
            i = MathUtils.clip(i, 0, position.length - 1);
            int inext = Math.min(i + 1, position.length - 1);

            long tstart = position[i].milli;
            long tend = position[inext].milli;

            double alpha = tend == tstart ? 1. : ((time - tstart) / (double) (tend - tstart)) % 1.;
            x = (1. - alpha) * position[i].x + alpha * position[inext].x;
            y = (1. - alpha) * position[i].y + alpha * position[inext].y;
            z = (1. - alpha) * position[i].z + alpha * position[inext].z;
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
        return new Vec3(dist, hgln, hglt);
    }

    public double getInterpolated(float[] xyz, long t, long start, long end) {
        long time = interpolateTime(t, start, end);

        double x, y, z;
        if (positionStart == positionEnd) {
            x = position[0].x;
            y = position[0].y;
            z = position[0].z;
        } else {
            double interpolatedIndex = (time - positionStart) / (double) (positionEnd - positionStart) * position.length;
            int i = (int) interpolatedIndex;
            i = MathUtils.clip(i, 0, position.length - 1);
            int inext = Math.min(i + 1, position.length - 1);

            long tstart = position[i].milli;
            long tend = position[inext].milli;

            double alpha = tend == tstart ? 1. : ((time - tstart) / (double) (tend - tstart)) % 1.;
            x = (1. - alpha) * position[i].x + alpha * position[inext].x;
            y = (1. - alpha) * position[i].y + alpha * position[inext].y;
            z = (1. - alpha) * position[i].z + alpha * position[inext].z;
        }
        xyz[0] = (float) x;
        xyz[1] = (float) y;
        xyz[2] = (float) z;

        return Math.sqrt(x * x + y * y + z * z);
    }

}
