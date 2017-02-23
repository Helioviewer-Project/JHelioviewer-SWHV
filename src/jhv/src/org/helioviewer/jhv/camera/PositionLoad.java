package org.helioviewer.jhv.camera;

import java.io.IOException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Iterator;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.DownloadStream;
import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.threads.JHVWorker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PositionLoad {

    private static final String LOADEDSTATE = "Loaded";
    private static final String FAILEDSTATE = "Failed";

    private String observer = "Earth";

    private long beginTime = TimeUtils.EPOCH.milli;
    private long endTime = TimeUtils.EPOCH.milli;

    private boolean isLoaded = false;
    @NotNull
    private Position.L[] position = new Position.L[0];
    private JHVWorker<Position.L[], Void> worker;

    private final PositionLoadFire receiver;

    public PositionLoad(PositionLoadFire _receiver) {
        receiver = _receiver;
    }

    private class LoadPositionWorker extends JHVWorker<Position.L[], Void> {

        private static final String baseURL = "http://swhv.oma.be/position?";
        private static final String target = "SUN";

        @Nullable
        private String report = null;

        private final long start;
        private final long end;
        private final String obs;

        public LoadPositionWorker(long _start, long _end, String _obs) {
            start = _start;
            end = _end;
            obs = _obs;
        }

        private String buildURL(long deltat) {
            return baseURL + "abcorr=LT%2BS&utc=" + TimeUtils.utcFullDateFormat.format(start) +
                   "&utc_end=" + TimeUtils.utcFullDateFormat.format(end) + "&deltat=" + deltat +
                   "&observer=" + obs + "&target=" + target + "&ref=HEEQ&kind=latitudinal";
        }

        @Override
        protected Position.L[] backgroundWork() {
            long deltat = 60, span = (end - start) / 1000;
            long max = 100000;

            if (span / deltat > max)
                deltat = span / max;

            try {
                DownloadStream ds = new DownloadStream(buildURL(deltat), true);
                JSONObject result = JSONUtils.getJSONStream(ds.getInput());
                if (ds.isResponse400()) {
                    report = result.has("faultstring") ? result.getString("faultstring") : "Invalid network response";
                } else {
                    return parseData(result);
                }
            } catch (UnknownHostException e) {
                Log.debug("Unknown host, network down?", e);
            } catch (IOException e) {
                report = FAILEDSTATE + ": server error";
            } catch (@NotNull JSONException | ParseException | NumberFormatException e) {
                report = FAILEDSTATE + ": JSON parse error";
            }

            return null;
        }

        @NotNull
        private Position.L[] parseData(@NotNull JSONObject jsonResult) throws JSONException, ParseException {
            JSONArray resArray = jsonResult.getJSONArray("result");
            int resLength = resArray.length();
            Position.L[] ret = new Position.L[resLength];

            for (int j = 0; j < resLength; j++) {
                JSONObject posObject = resArray.getJSONObject(j);
                Iterator<String> iterKeys = posObject.keys();
                if (!iterKeys.hasNext())
                    throw new JSONException("unexpected format");

                String dateString = iterKeys.next();
                JSONArray posArray = posObject.getJSONArray(dateString);

                double rad = posArray.getDouble(0) * (1000. / Sun.RadiusMeter);
                double jlon = posArray.getDouble(1);
                double lon = jlon + (jlon > 0 ? -Math.PI : Math.PI);
                double lat = -posArray.getDouble(2);

                JHVDate time = new JHVDate(TimeUtils.utcFullDateFormat.parse(dateString).getTime());
                Position.L p = Sun.getEarth(time);

                ret[j] = new Position.L(time, rad, -lon + p.lon, lat);
            }
            return ret;
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
                        setLoaded(true);
                    }
                }
                if (report != null) {
                    receiver.fireLoaded(report);
                    report = null;
                }
            }
        }
    }

    private void setLoaded(boolean _isLoaded) {
        isLoaded = _isLoaded;
        if (isLoaded) {
            receiver.fireLoaded(LOADEDSTATE);
        }
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    private void applyChanges() {
        setLoaded(false);
        position = new Position.L[0];

        if (worker != null) {
            worker.cancel(false);
        }
        receiver.fireLoaded("Loading...");

        worker = new LoadPositionWorker(beginTime, endTime, observer);
        worker.setThreadName("MAIN--PositionLoad");
        JHVGlobals.getExecutorService().execute(worker);
    }

    public void setBeginTime(long _beginTime, boolean applyChanges) {
        beginTime = _beginTime;
        if (applyChanges) {
            applyChanges();
        }
    }

    public void setEndTime(long _endTime, boolean applyChanges) {
        endTime = _endTime;
        if (applyChanges) {
            applyChanges();
        }
    }

    public long getStartTime() {
        if (position.length > 0) {
            return position[0].time.milli;
        }
        return -1L;
    }

    public long getEndTime() {
        if (position.length > 0) {
            return position[position.length - 1].time.milli;
        }
        return -1L;
    }

    @Nullable
    public Position.Q getInterpolatedPosition(long currentCameraTime) {
        if (isLoaded && position.length > 0) {
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
        } else {
            return null;
        }
    }

    public void setObserver(String object, boolean applyChanges) {
        observer = object;
        if (applyChanges) {
            applyChanges();
        }
    }

}
