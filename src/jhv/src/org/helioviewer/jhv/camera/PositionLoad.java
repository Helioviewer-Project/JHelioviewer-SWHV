package org.helioviewer.jhv.camera;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.DownloadStream;
import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.threads.JHVWorker;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class PositionLoad {

    private static final String LOADEDSTATE = "Loaded";
    private static final String FAILEDSTATE = "Failed";

    private static final String baseUrl = "http://swhv.oma.be/position?";
    private static final String target = "SUN";
    private String observer = "Earth";

    private String beginDate = "2014-05-28T00:00:00";
    private String endDate = "2014-05-28T00:00:00";
    private Date beginDatems = new Date(0);
    private Date endDatems = new Date();

    private boolean isLoaded = false;
    private Position.Latitudinal[] position;
    private JHVWorker<Position.Latitudinal[], Void> worker;

    private final ViewpointExpert viewpoint;

    public PositionLoad(ViewpointExpert _viewpoint) {
        viewpoint = _viewpoint;
    }

    private class LoadPositionWorker extends JHVWorker<Position.Latitudinal[], Void> {

        private String report = null;
        private final String beginDate;
        private final String endDate;
        private final Date beginDatems;
        private final Date endDatems;
        private final String observer;

        public LoadPositionWorker(String _beginDate, String _endDate, Date _beginDatems, Date _endDatems, String _observer) {
            beginDate = _beginDate;
            endDate = _endDate;
            beginDatems = _beginDatems;
            endDatems = _endDatems;
            observer = _observer;
        }

        @Override
        protected Position.Latitudinal[] backgroundWork() throws Exception {
            Position.Latitudinal[] ret = null;
            JSONObject result;
            try {
                long deltat = 60, span = (endDatems.getTime() - beginDatems.getTime()) / 1000;
                final long max = 100000;

                if (span / deltat > max)
                    deltat = span / max;

                URL url = new URL(baseUrl + "abcorr=LT%2BS&utc=" + beginDate + "&utc_end=" + endDate + "&deltat=" + deltat +
                                            "&observer=" + observer + "&target=" + target + "&ref=HEEQ&kind=latitudinal");
                DownloadStream ds = new DownloadStream(url.toURI(), 30000, 30000, true);
                Reader reader = new BufferedReader(new InputStreamReader(ds.getInput(), "UTF-8"));
                if (!ds.getResponse400()) {
                    result = new JSONObject(new JSONTokener(reader));
                    ret = parseData(result);
                } else {
                    JSONObject jsonObject = new JSONObject(new JSONTokener(reader));
                    if (jsonObject.has("faultstring")) {
                        report = jsonObject.getString("faultstring");
                    } else {
                        report = "Invalid network response";
                    }
                }
            } catch (MalformedURLException e) {
                Log.error("Wrong URL", e);
            } catch (UnknownHostException e) {
                Log.debug("Unknown host, network down?", e);
            } catch (IOException e) {
                report = FAILEDSTATE + ": server error";
            } catch (JSONException e) {
                report = FAILEDSTATE + ": JSON parse error";
            } catch (ParseException e) {
                report = FAILEDSTATE + ": JSON parse error";
            } catch (NumberFormatException e) {
                report = FAILEDSTATE + ": JSON parse error";
            } catch (URISyntaxException e) {
                report = FAILEDSTATE + ": wrong URI";
            } finally {
                result = null;
            }

            return ret;
        }

        private Position.Latitudinal[] parseData(JSONObject jsonResult) throws JSONException, ParseException {
            JSONArray resArray = jsonResult.getJSONArray("result");
            int resLength = resArray.length();
            Position.Latitudinal[] ret = new Position.Latitudinal[resLength];

            for (int j = 0; j < resLength; j++) {
                JSONObject posObject = resArray.getJSONObject(j);
                Iterator<?> iterKeys = posObject.keys();
                if (!iterKeys.hasNext())
                    throw new JSONException("unexpected format");

                String dateString = (String) iterKeys.next();
                JSONArray posArray = posObject.getJSONArray(dateString);

                double rad, lon, lat, jlon;
                rad = posArray.getDouble(0) * (1000. / Sun.RadiusMeter);
                jlon = posArray.getDouble(1);
                lon = jlon + (jlon > 0 ? -Math.PI : Math.PI);
                lat = -posArray.getDouble(2);

                Date date = TimeUtils.utcFullDateFormat.parse(dateString);
                ret[j] = new Position.Latitudinal(date.getTime(), rad, lon, lat);
            }
            return ret;
        }

        @Override
        protected void done() {
            if (!isCancelled()) {
                Position.Latitudinal[] newPosition = null;
                try {
                    newPosition = get();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (report == null) {
                    if (newPosition == null) {
                        report = "response is void";
                    } else if (newPosition.length == 0) {
                        report = "response is zero length array";
                    } else {
                        position = newPosition;
                        setLoaded(true);
                    }
                }
                if (report != null) {
                    fireLoaded(report);
                    report = null;
                }
            }
        }
    }

    private void requestData() {
        if (worker != null) {
            worker.cancel(false);
        }
        fireLoaded("Loading...");

        worker = new LoadPositionWorker(beginDate, endDate, beginDatems, endDatems, observer);
        worker.setThreadName("MAIN--GL3DPositionLoading");
        JHVGlobals.getExecutorService().execute(worker);
    }

    private void setLoaded(boolean _isLoaded) {
        isLoaded = _isLoaded;
        if (isLoaded) {
            fireLoaded(LOADEDSTATE);
        }
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    private void applyChanges() {
        setLoaded(false);
        position = null;
        requestData();
    }

    public void setBeginDate(Date _beginDate, boolean applyChanges) {
        beginDate = TimeUtils.utcFullDateFormat.format(_beginDate);
        beginDatems = _beginDate;
        if (applyChanges) {
            applyChanges();
        }
    }

    public void setEndDate(Date _endDate, boolean applyChanges) {
        endDate = TimeUtils.utcFullDateFormat.format(_endDate);
        endDatems = _endDate;
        if (applyChanges) {
            applyChanges();
        }
    }

    public void fireLoaded(String state) {
        viewpoint.firePositionLoaded(state);
    }

    public Date getBeginDate() {
        return beginDatems;
    }

    public Date getEndDate() {
        return endDatems;
    }

    public long getStartTime() {
        if (position.length > 0) {
            return position[0].milli;
        }
        return -1L;
    }

    public long getEndTime() {
        if (position.length > 0) {
            return position[position.length - 1].milli;
        }
        return -1L;
    }

    public Position.Latitudinal getInterpolatedPosition(long currentCameraTime) {
        if (isLoaded && position.length > 0) {
            double dist, hgln, hglt;
            long milli;

            long tstart = getStartTime();
            long tend = getEndTime();
            if (tstart == tend) {
                milli = position[0].milli;
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

                tstart = position[i].milli;
                tend = position[inext].milli;

                double alpha;
                if (tend == tstart)
                    alpha = 1.;
                else
                    alpha = ((currentCameraTime - tstart) / (double) (tend - tstart)) % 1.;

                milli = (long) ((1. - alpha) * position[i].milli + alpha * position[inext].milli);
                dist = (1. - alpha) * position[i].rad + alpha * position[inext].rad;
                hgln = (1. - alpha) * position[i].lon + alpha * position[inext].lon;
                hglt = (1. - alpha) * position[i].lat + alpha * position[inext].lat;
            }
            return new Position.Latitudinal(milli, dist, hgln, hglt);
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
