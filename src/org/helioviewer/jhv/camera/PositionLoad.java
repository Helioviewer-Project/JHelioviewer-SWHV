package org.helioviewer.jhv.camera;

import java.io.IOException;
import java.net.UnknownHostException;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.io.DownloadStream;
import org.helioviewer.jhv.io.PositionRequest;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.math.MathUtils;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.time.JHVDate;
import org.helioviewer.jhv.time.TimeUtils;
import org.json.JSONObject;

public class PositionLoad {

    private String target = "Earth";

    private long beginTime = TimeUtils.EPOCH.milli;
    private long endTime = TimeUtils.EPOCH.milli;

    private Position.L[] position = new Position.L[0];
    private JHVWorker<Position.L[], Void> worker;

    private final PositionLoadFire receiver;
    private final String frame;

    public PositionLoad(PositionLoadFire _receiver, String _frame) {
        receiver = _receiver;
        frame = _frame;
    }

    private class LoadPositionWorker extends JHVWorker<Position.L[], Void> {

        private String report = null;

        private final String tgt;
        private final long start;
        private final long end;

        public LoadPositionWorker(String _tgt, long _start, long _end) {
            tgt = _tgt;
            start = _start;
            end = _end;
        }

        @Override
        protected Position.L[] backgroundWork() {
            long deltat = 60, span = (end - start) / 1000;
            long max = 100000;

            if (span / deltat > max)
                deltat = span / max;

            try {
                DownloadStream ds = new DownloadStream(new PositionRequest(tgt, frame, start, end, deltat).url, true);
                JSONObject result = JSONUtils.getJSONStream(ds.getInput());
                if (ds.isResponse400()) {
                    report = result.optString("faultstring", "Invalid network response");
                } else {
                    return PositionRequest.parseResponse(result);
                }
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
            if (!isCancelled()) {
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
                if (report != null) {
                    receiver.fireLoaded(report);
                    report = null;
                }
            }
        }
    }

    public boolean isLoaded() {
        return position.length > 0;
    }

    private void applyChanges() {
        position = new Position.L[0];
        if (worker != null)
            worker.cancel(false);
        receiver.fireLoaded("Loading...");

        worker = new LoadPositionWorker(target, beginTime, endTime);
        worker.setThreadName("MAIN--PositionLoad");
        JHVGlobals.getExecutorService().execute(worker);
    }

    public long interpolateTime(long time, long start, long end) {
        long pStart = position[0].time.milli;
        long pEnd = position[position.length - 1].time.milli;
        if (start == end)
            return pEnd;
        else {
            double f = (time - start) / (double) (end - start); //!
            return (long) (pStart + f * (pEnd - pStart) + .5);
        }
    }

    public Position.L getInterpolatedL(long time) {
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

    public Position.Q getInterpolatedQ(long time) {
        Position.L p = getInterpolatedL(time);
        Position.L e = Sun.getEarth(p.time);
        return new Position.Q(p.time, p.rad, new Quat(p.lat, e.lon - p.lon));
    }

    public void setTarget(String object, boolean applyChanges) {
        target = object;
        if (applyChanges)
            applyChanges();
    }

    public void setBeginTime(long _beginTime, boolean applyChanges) {
        beginTime = _beginTime;
        if (applyChanges)
            applyChanges();
    }

    public void setEndTime(long _endTime, boolean applyChanges) {
        endTime = _endTime;
        if (applyChanges)
            applyChanges();
    }

}
