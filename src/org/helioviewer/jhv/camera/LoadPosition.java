package org.helioviewer.jhv.camera;

import java.io.IOException;
import java.net.UnknownHostException;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.io.NetClient;
import org.helioviewer.jhv.io.PositionRequest;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.time.JHVDate;
import org.json.JSONObject;

public class LoadPosition extends JHVWorker<Position.L[], Void> {

    private final LoadPositionFire receiver;
    private final SpaceObject target;
    private final String frame;
    private final long start;
    private final long end;

    private Position.L[] position = new Position.L[0];
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
    protected Position.L[] backgroundWork() {
        long deltat = 60, span = (end - start) / 1000;
        long max = 100000;

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

        Position.L[] newPosition = null;
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

    private long interpolateTime(long t, long startTime, long endTime) {
        long pStart = position[0].time.milli;
        long pEnd = position[position.length - 1].time.milli;
        if (startTime == endTime)
            return pEnd;
        else {
            double f = (t - startTime) / (double) (endTime - startTime); //!
            return (long) (pStart + f * (pEnd - pStart) + .5);
        }
    }

    public Position.L getInterpolatedL(long t, long startTime, long endTime) {
        long time = interpolateTime(t, startTime, endTime);
        double dist, hgln, hglt;
        long tstart = position[0].time.milli;
        long tend = position[position.length - 1].time.milli;
        if (tstart == tend) {
            dist = position[0].rad;
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
            dist = (1. - alpha) * position[i].rad + alpha * position[inext].rad;
            hgln = (1. - alpha) * position[i].lon + alpha * position[inext].lon;
            hglt = (1. - alpha) * position[i].lat + alpha * position[inext].lat;
        }
        return new Position.L(new JHVDate(time), dist, hgln, hglt);
    }

    public Position.Q getInterpolatedQ(long t, long startTime, long endTime) {
        Position.L p = getInterpolatedL(t, startTime, endTime);
        Position.L e = Sun.getEarth(p.time);
        return new Position.Q(p.time, p.rad, new Quat(p.lat, e.lon - p.lon));
    }

}
