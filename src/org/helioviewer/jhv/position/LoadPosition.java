package org.helioviewer.jhv.position;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Iterator;

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
import org.helioviewer.jhv.time.TimeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class LoadPosition extends JHVWorker<PositionCartesian[], Void> {

    private static final String baseURL = "http://swhv.oma.be/position?";
    private static final String observer = "SUN";

    private final LoadPositionFire receiver;
    private final SpaceObject target;
    private final String frame;
    private final long start;
    private final long end;

    private PositionCartesian[] position = new PositionCartesian[0];
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
    protected PositionCartesian[] backgroundWork() {
        long deltat = 60, span = (end - start) / 1000;
        long max = 10000;

        if (span / deltat > max)
            deltat = span / max;

        try (NetClient nc = NetClient.of(getURL(target, frame, start, end, deltat), true)) {
            JSONObject result = JSONUtils.get(nc.getReader());
            if (nc.isSuccessful())
                return parseResponse(result);
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

        PositionCartesian[] newPosition = null;
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

    private static String getURL(SpaceObject _target, String _frame, long _start, long _end, long _deltat) {
        return baseURL + "ref=" + _frame + "&observer=" + observer + "&target=" + _target.getUrlName() +
               "&utc=" + TimeUtils.format(_start) + "&utc_end=" + TimeUtils.format(_end) + "&deltat=" + _deltat;
    }

    private static PositionCartesian[] parseResponse(JSONObject jo) throws Exception {
        JSONArray res = jo.getJSONArray("result");
        int len = res.length();
        PositionCartesian[] ret = new PositionCartesian[len];

        for (int j = 0; j < len; j++) {
            JSONObject posObject = res.getJSONObject(j);
            Iterator<String> iterKeys = posObject.keys();
            if (!iterKeys.hasNext())
                throw new Exception("unexpected format");

            String date = iterKeys.next();
            JSONArray posArray = posObject.getJSONArray(date);

            double x = posArray.getDouble(0) * Sun.RadiusKMeterInv;
            double y = posArray.getDouble(1) * Sun.RadiusKMeterInv;
            double z = posArray.getDouble(2) * Sun.RadiusKMeterInv;
            ret[j] = new PositionCartesian(TimeUtils.parse(date), x, y, z);
        }
        return ret;
    }

    public SpaceObject getTarget() {
        return target;
    }

    public boolean isLoaded() {
        return position.length > 0;
    }

    public long interpolateTime(long t, long startTime, long endTime) {
        long pStart = position[0].milli;
        long pEnd = position[position.length - 1].milli;
        if (startTime == endTime)
            return pEnd;
        else {
            double f = (t - startTime) / (double) (endTime - startTime); //!
            return (long) (pStart + f * (pEnd - pStart) + .5);
        }
    }

    public Position getRelativeInterpolated(long t, long startTime, long endTime) {
        long time = interpolateTime(t, startTime, endTime);
        long tstart = position[0].milli;
        long tend = position[position.length - 1].milli;

        double x, y, z;
        if (tstart == tend) {
            x = position[0].x;
            y = position[0].y;
            z = position[0].z;
        } else {
            double interpolatedIndex = (time - tstart) / (double) (tend - tstart) * position.length;
            int i = (int) interpolatedIndex;
            i = MathUtils.clip(i, 0, position.length - 1);
            int inext = Math.min(i + 1, position.length - 1);

            tstart = position[i].milli;
            tend = position[inext].milli;

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

    public Vec3 getInterpolatedHG(long t, long startTime, long endTime) {
        long time = interpolateTime(t, startTime, endTime);
        long tstart = position[0].milli;
        long tend = position[position.length - 1].milli;

        double x, y, z;
        if (tstart == tend) {
            x = position[0].x;
            y = position[0].y;
            z = position[0].z;
        } else {
            double interpolatedIndex = (time - tstart) / (double) (tend - tstart) * position.length;
            int i = (int) interpolatedIndex;
            i = MathUtils.clip(i, 0, position.length - 1);
            int inext = Math.min(i + 1, position.length - 1);

            tstart = position[i].milli;
            tend = position[inext].milli;

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

    public double getInterpolatedArray(FloatArray array, long t, long startTime, long endTime) {
        long time = interpolateTime(t, startTime, endTime);
        long tstart = position[0].milli;
        long tend = position[position.length - 1].milli;

        double x, y, z;
        if (tstart == tend) {
            x = position[0].x;
            y = position[0].y;
            z = position[0].z;
        } else {
            double interpolatedIndex = (time - tstart) / (double) (tend - tstart) * position.length;
            int i = (int) interpolatedIndex;
            i = MathUtils.clip(i, 0, position.length - 1);
            int inext = Math.min(i + 1, position.length - 1);

            tstart = position[i].milli;
            tend = position[inext].milli;

            double alpha = tend == tstart ? 1. : ((time - tstart) / (double) (tend - tstart)) % 1.;
            x = (1. - alpha) * position[i].x + alpha * position[inext].x;
            y = (1. - alpha) * position[i].y + alpha * position[inext].y;
            z = (1. - alpha) * position[i].z + alpha * position[inext].z;
        }
        array.put3f((float) x, (float) y, (float) z);
        return Math.sqrt(x * x + y * y + z * z);
    }

}
