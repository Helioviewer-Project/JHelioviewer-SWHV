package org.helioviewer.jhv.astronomy;

import java.util.Iterator;

import javax.annotation.Nullable;

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

        private double longitude(double dist) {
            return dist == 0 ? 0 : Math.atan2(y, x);
        }

        private double latitude(double dist) {
            return dist == 0 ? 0 : Math.asin(Math.clamp(z / dist, -1., 1.));
        }
    }

    private final Position.Cartesian[] position;
    @Nullable
    private final Position.Cartesian[] velocity; // units of c
    private final long positionStart;
    private final long positionEnd;

    PositionResponse(Position.Cartesian[] _position, @Nullable Position.Cartesian[] _velocity) throws Exception {
        int len = _position.length;
        if (len == 0)
            throw new Exception("Empty response");
        position = _position;
        velocity = _velocity;
        positionStart = position[0].milli();
        positionEnd = position[len - 1].milli();
    }

    PositionResponse(JSONObject jo, boolean state) throws Exception {
        JSONArray res = jo.getJSONArray("result");
        int len = res.length();
        if (len == 0)
            throw new Exception("Empty response");

        position = new Position.Cartesian[len];
        velocity = state ? new Position.Cartesian[len] : null;
        for (int i = 0; i < len; i++) {
            JSONObject posObject = res.getJSONObject(i);
            Iterator<String> iterKeys = posObject.keys();
            if (!iterKeys.hasNext())
                throw new Exception("Unexpected format");

            String date = iterKeys.next();
            long milli = TimeUtils.parse(date);
            JSONArray posArray = posObject.getJSONArray(date);
            if (state) {
                JSONArray velArray = posArray.getJSONArray(1);
                posArray = posArray.getJSONArray(0);
                double vx = velArray.getDouble(0) * Spice.VelocityFactor;
                double vy = velArray.getDouble(1) * Spice.VelocityFactor;
                double vz = velArray.getDouble(2) * Spice.VelocityFactor;
                velocity[i] = new Position.Cartesian(milli, vx, vy, vz);
            }

            double x = posArray.getDouble(0) * Sun.RadiusKMeterInv;
            double y = posArray.getDouble(1) * Sun.RadiusKMeterInv;
            double z = posArray.getDouble(2) * Sun.RadiusKMeterInv;
            position[i] = new Position.Cartesian(milli, x, y, z);
        }
        positionStart = position[0].milli();
        positionEnd = position[len - 1].milli();
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

    private void interpolate(long t, long start, long end, Interpolated out, @Nullable double[] vel) {
        long time = interpolateTime(t, start, end);
        out.time = time;

        int i = 0, inext = 0;
        double alpha = 1.;
        if (positionStart != positionEnd) {
            int maxIndex = position.length - 1;
            double interpolatedIndex = (time - positionStart) / (double) (positionEnd - positionStart) * maxIndex;
            i = Math.clamp((int) interpolatedIndex, 0, maxIndex);
            inext = Math.min(i + 1, maxIndex);

            long tstart = position[i].milli();
            long tend = position[inext].milli();
            alpha = tend == tstart ? 1. : Math.clamp((time - tstart) / (double) (tend - tstart), 0, 1);
        }

        // Position and velocity samples share timestamps.
        if (vel != null) {
            interpolate(velocity, i, inext, alpha, out);
            vel[0] = out.x;
            vel[1] = out.y;
            vel[2] = out.z;
        }
        interpolate(position, i, inext, alpha, out);
    }

    private void interpolate(Position.Cartesian[] samples, int i, int inext, double alpha, Interpolated out) {
        if (positionStart == positionEnd) {
            Position.Cartesian p = samples[0];
            out.x = p.x();
            out.y = p.y();
            out.z = p.z();
            return;
        }

        out.x = (1. - alpha) * samples[i].x() + alpha * samples[inext].x();
        out.y = (1. - alpha) * samples[i].y() + alpha * samples[inext].y();
        out.z = (1. - alpha) * samples[i].z() + alpha * samples[inext].z();
    }

    // position and velocity (units of c) of a state response
    public void interpolateState(long t, long start, long end, double[] pos, double[] vel, Interpolated out) {
        if (velocity == null)
            throw new IllegalStateException("Not a state response");
        interpolate(t, start, end, out, vel);
        pos[0] = out.x;
        pos[1] = out.y;
        pos[2] = out.z;
    }

    public double interpolateRectangular(long t, long start, long end, float[] xyz, Interpolated out) {
        interpolate(t, start, end, out, null);

        xyz[0] = (float) out.x;
        xyz[1] = (float) out.y;
        xyz[2] = (float) out.z;
        return out.distance();
    }

    public double interpolateRectangular(long t, long start, long end, double[] xyz, Interpolated out) {
        interpolate(t, start, end, out, null);

        xyz[0] = out.x;
        xyz[1] = out.y;
        xyz[2] = out.z;
        return out.distance();
    }

    public void interpolateLatitudinal(long t, long start, long end, double[] lati, Interpolated out) {
        interpolate(t, start, end, out, null);
        double dist = out.distance();
        lati[0] = dist;
        lati[1] = out.longitude(dist);
        lati[2] = out.latitude(dist);
    }

    Position interpolateCarrington(long t, long start, long end, Interpolated out) {
        interpolate(t, start, end, out, null);
        double dist = out.distance();
        double hgln = out.longitude(dist);
        if (hgln < 0)
            hgln += 2 * Math.PI;
        return new Position(new JHVTime(out.time), dist, -hgln, out.latitude(dist));
    }

}
