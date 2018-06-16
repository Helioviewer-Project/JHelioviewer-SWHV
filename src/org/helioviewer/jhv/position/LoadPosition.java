package org.helioviewer.jhv.position;

import java.io.IOException;
import java.net.UnknownHostException;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.FloatArray;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.time.JHVDate;
import org.json.JSONObject;

public class LoadPosition extends JHVWorker<Position[], Void> {

    private final LoadPositionFire receiver;
    private final SpaceObject target;
    private final String frame;
    private final long start;
    private final long end;

    private Position[] position = new Position[0];
    private String report = null;

    public LoadPosition(LoadPositionFire _receiver, SpaceObject _target, String _frame, long _start, long _end) {
        receiver = _receiver;
        target = _target;
        frame = _frame;
        start = _start;
        end = _end;
        receiver.fireLoaded("Loading...");
        setThreadName("MAIN--PositionLoad");
    }

    @Nullable
    @Override
    protected Position[] backgroundWork() {
        long deltat = 60, span = (end - start) / 1000;
        long max = 10000;

        if (span / deltat > max)
            deltat = span / max;

        try (NetClient nc = NetClient.of(new PositionRequest(target, frame, start, end, deltat).url, true)) {
            JSONObject result = JSONUtils.get(nc.getReader());
            if (nc.isSuccessful())
                return PositionRequest.parseResponse(result);
            else
                report = result.optString("faultstring", "Invalid network response");
        } catch (UnknownHostException e) {
            Log.debug("Unknown host, network down?", e);
        } catch (IOException e) {
            report = "Failed: server error";
        } catch (Exception e) {
            report = "Failed: JSON parse error: " + e;
        }
        return null;
    }

    @Override
    protected void done() {
        if (isCancelled()) {
            receiver.fireLoaded("Cancelled");
            return;
        }

        Position[] newPosition = null;
        try {
            newPosition = get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (report == null) {
            if (newPosition == null || newPosition.length == 0) {
                report = "empty response";
            } else {
                position = newPosition;
                receiver.fireLoaded("Loaded");
            }
        }
        if (report != null)
            receiver.fireLoaded(report);
    }

    public SpaceObject getTarget() {
        return target;
    }

    public boolean isLoaded() {
        return position.length > 0;
    }

    public long interpolateTime(long t, long start, long end) {
        long pStart = position[0].time.milli;
        long pEnd = position[position.length - 1].time.milli;
        if (start == end)
            return pEnd;
        else {
            double f = (t - start) / (double) (end - start); //!
            return (long) (pStart + f * (pEnd - pStart) + .5);
        }
    }

    private Position getInterpolated(long t, long startTime, long endTime) {
        long time = interpolateTime(t, startTime, endTime);
        double dist, hgln, hglt;
        long tstart = position[0].time.milli;
        long tend = position[position.length - 1].time.milli;
        if (tstart == tend) {
            dist = position[0].distance;
            hgln = position[0].lon;
            hglt = position[0].lat;
        } else {
            double interpolatedIndex = (time - tstart) / (double) (tend - tstart) * position.length;
            int i = (int) interpolatedIndex;
            i = MathUtils.clip(i, 0, position.length - 1);
            int inext = Math.min(i + 1, position.length - 1);

            tstart = position[i].time.milli;
            tend = position[inext].time.milli;

            double alpha = tend == tstart ? 1. : ((time - tstart) / (double) (tend - tstart)) % 1.;

            double xi = position[i].distance * Math.cos(position[i].lat) * Math.cos(position[i].lon);
            double yi = position[i].distance * Math.cos(position[i].lat) * Math.sin(position[i].lon);
            double zi = position[i].distance * Math.sin(position[i].lat);

            double xin = position[inext].distance * Math.cos(position[inext].lat) * Math.cos(position[inext].lon);
            double yin = position[inext].distance * Math.cos(position[inext].lat) * Math.sin(position[inext].lon);
            double zin = position[inext].distance * Math.sin(position[inext].lat);

            double ix = (1. - alpha) * xi + alpha * xin;
            double iy = (1. - alpha) * yi + alpha * yin;
            double iz = (1. - alpha) * zi + alpha * zin;

            dist = Math.sqrt(ix * ix + iy * iy + iz * iz);
            if (dist == 0) {
                hgln = 0;
                hglt = 0;
            } else {
                hgln = Math.atan2(iy, ix);
                hglt = Math.asin(iz / dist);
            }
        }
        return new Position(new JHVDate(time), dist, hgln, hglt);
    }

    public Vec3 getInterpolatedHG(long t, long startTime, long endTime) {
        long time = interpolateTime(t, startTime, endTime);
        double dist, hgln, hglt;
        long tstart = position[0].time.milli;
        long tend = position[position.length - 1].time.milli;
        if (tstart == tend) {
            dist = position[0].distance;
            hgln = position[0].lon;
            hglt = position[0].lat;
        } else {
            double interpolatedIndex = (time - tstart) / (double) (tend - tstart) * position.length;
            int i = (int) interpolatedIndex;
            i = MathUtils.clip(i, 0, position.length - 1);
            int inext = Math.min(i + 1, position.length - 1);

            tstart = position[i].time.milli;
            tend = position[inext].time.milli;

            double alpha = tend == tstart ? 1. : ((time - tstart) / (double) (tend - tstart)) % 1.;

            double xi = position[i].distance * Math.cos(position[i].lat) * Math.cos(position[i].lon);
            double yi = position[i].distance * Math.cos(position[i].lat) * Math.sin(position[i].lon);
            double zi = position[i].distance * Math.sin(position[i].lat);

            double xin = position[inext].distance * Math.cos(position[inext].lat) * Math.cos(position[inext].lon);
            double yin = position[inext].distance * Math.cos(position[inext].lat) * Math.sin(position[inext].lon);
            double zin = position[inext].distance * Math.sin(position[inext].lat);

            double ix = (1. - alpha) * xi + alpha * xin;
            double iy = (1. - alpha) * yi + alpha * yin;
            double iz = (1. - alpha) * zi + alpha * zin;

            dist = Math.sqrt(ix * ix + iy * iy + iz * iz);
            if (dist == 0) {
                hgln = 0;
                hglt = 0;
            } else {
                hgln = Math.atan2(iy, ix);
                hglt = Math.asin(iz / dist);
            }
        }
        return new Vec3(dist, hgln, hglt);
    }

    public double getInterpolatedArray(FloatArray array, long t, long startTime, long endTime) {
        long time = interpolateTime(t, startTime, endTime);
        double dist, hgln, hglt;
        long tstart = position[0].time.milli;
        long tend = position[position.length - 1].time.milli;
        if (tstart == tend) {
            dist = position[0].distance;
            hgln = position[0].lon;
            hglt = position[0].lat;
            array.put3f((float) (dist * Math.cos(hglt) * Math.cos(hgln)),
                        (float) (dist * Math.cos(hglt) * Math.sin(hgln)),
                        (float) (dist * Math.sin(hglt)));
        } else {
            double interpolatedIndex = (time - tstart) / (double) (tend - tstart) * position.length;
            int i = (int) interpolatedIndex;
            i = MathUtils.clip(i, 0, position.length - 1);
            int inext = Math.min(i + 1, position.length - 1);

            tstart = position[i].time.milli;
            tend = position[inext].time.milli;

            double alpha = tend == tstart ? 1. : ((time - tstart) / (double) (tend - tstart)) % 1.;
            dist = (1. - alpha) * position[i].distance + alpha * position[inext].distance;

            double xi = position[i].distance * Math.cos(position[i].lat) * Math.cos(position[i].lon);
            double yi = position[i].distance * Math.cos(position[i].lat) * Math.sin(position[i].lon);
            double zi = position[i].distance * Math.sin(position[i].lat);

            double xin = position[inext].distance * Math.cos(position[inext].lat) * Math.cos(position[inext].lon);
            double yin = position[inext].distance * Math.cos(position[inext].lat) * Math.sin(position[inext].lon);
            double zin = position[inext].distance * Math.sin(position[inext].lat);

            array.put3f((float) ((1. - alpha) * xi + alpha * xin),
                        (float) ((1. - alpha) * yi + alpha * yin),
                        (float) ((1. - alpha) * zi + alpha * zin));
        }
        return dist;
    }

    public Position getRelativeInterpolated(long t, long startTime, long endTime) {
        Position p = getInterpolated(t, startTime, endTime);
        double elon = Sun.getEarth(p.time /*!*/).lon;
        return new Position(p.time, p.distance, elon - p.lon, p.lat);
    }

    @Override
    public String toString() {
        return "LoadPosition " + target + " " + new JHVDate(start) + " " + new JHVDate(end);
    }

}
