package org.helioviewer.jhv.camera;

import java.io.IOException;
import java.net.UnknownHostException;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.io.DownloadStream;
import org.helioviewer.jhv.io.PositionRequest;
import org.helioviewer.jhv.log.Log;
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

    public PositionLoad(PositionLoadFire _receiver) {
        receiver = _receiver;
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
                DownloadStream ds = new DownloadStream(new PositionRequest(tgt, start, end, deltat).url, true);
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


    public Position.Q getInterpolatedPosition(long currentCameraTime) {
        if (position.length > 0) {
            double dist, hgln, hglt;
            long milli;

            long tstart = getStartTime();
            long tend = getEndTime();
            if (tstart == tend) {
                milli = position[0].time.milli;
                dist = position[0].rad;
                hgln = position[0].lon;
                hglt = position[0].lat;
            } else {
                double interpolatedIndex = (currentCameraTime - tstart) / (double) (tend - tstart) * position.length;
                int i = (int) interpolatedIndex;
                i = Math.min(i, position.length - 1);
                if (i < 0) {
                    i = 0;
                }
                int inext = Math.min(i + 1, position.length - 1);

                tstart = position[i].time.milli;
                tend = position[inext].time.milli;

                double alpha = tend == tstart ? 1. : ((currentCameraTime - tstart) / (double) (tend - tstart)) % 1.;
                milli = (long) ((1. - alpha) * position[i].time.milli + alpha * position[inext].time.milli);
                dist = (1. - alpha) * position[i].rad + alpha * position[inext].rad;
                hgln = (1. - alpha) * position[i].lon + alpha * position[inext].lon;
                hglt = (1. - alpha) * position[i].lat + alpha * position[inext].lat;
            }
            return new Position.Q(new JHVDate(milli), dist, new Quat(hglt, hgln));
        } else
            return null;
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

    public long getStartTime() {
        if (position.length > 0)
            return position[0].time.milli;
        return -1L;
    }

    public long getEndTime() {
        if (position.length > 0)
            return position[position.length - 1].time.milli;
        return -1L;
    }

}
