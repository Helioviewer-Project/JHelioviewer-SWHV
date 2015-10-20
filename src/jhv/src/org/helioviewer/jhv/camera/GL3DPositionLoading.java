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

import javax.swing.SwingWorker;

import org.helioviewer.base.DownloadStream;
import org.helioviewer.base.astronomy.Position;
import org.helioviewer.base.astronomy.Sun;
import org.helioviewer.base.logging.Log;
import org.helioviewer.base.time.TimeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class GL3DPositionLoading {

    private static final String LOADEDSTATE = "Loaded";
    private static final String FAILEDSTATE = "Failed";
    private static final String PARTIALSTATE = "Partial";

    private static final String baseUrl = "http://swhv.oma.be/position?";
    private static final String target = "SUN";
    private String observer = "Earth";

    private String beginDate = "2014-05-28T00:00:00";
    private String endDate = "2014-05-28T00:00:00";
    private Date beginDatems = new Date(0);
    private Date endDatems = new Date();

    private boolean isLoaded = false;
    private Position.Latitudinal[] position;
    private SwingWorker<Void, Void> worker;
    private final GL3DExpertCamera camera;

    public GL3DPositionLoading(GL3DExpertCamera camera) {
        this.camera = camera;
    }

    private URL buildRequestURL(long deltat) {
        try {
            return new URL(baseUrl + "abcorr=LT%2BS&utc=" + this.beginDate + "&utc_end=" + this.endDate + "&deltat=" + deltat + "&observer=" + observer + "&target=" + target + "&ref=HEEQ&kind=latitudinal");
        } catch (MalformedURLException e) {
            Log.error("Wrong URL", e);
        }
        return null;
    }

    private void requestData() {
        if (worker != null) {
            worker.cancel(false);
        }
        fireLoaded("Loading...");

        worker = new SwingWorker<Void, Void>() {
            private String report = null;
            private JSONObject result = null;

            @Override
            protected Void doInBackground() throws Exception {
                Thread.currentThread().setName("GL3DPositionLoading--Main");
                try {
                    long deltat = 60, span = (endDatems.getTime() - beginDatems.getTime()) / 1000;
                    final long max = 100000;

                    if (span / deltat > max)
                        deltat = span / max;

                    URL url = buildRequestURL(deltat);
                    DownloadStream ds = new DownloadStream(url.toURI(), 30000, 30000, true);
                    Reader reader = new BufferedReader(new InputStreamReader(ds.getInput(), "UTF-8"));
                    if (!ds.getResponse400()) {
                        result = new JSONObject(new JSONTokener(reader));
                    } else {
                        JSONObject jsonObject = new JSONObject(new JSONTokener(reader));
                        if (jsonObject.has("faultstring")) {
                            report = jsonObject.getString("faultstring");
                        } else {
                            report = "Invalid network response";
                        }
                    }
                } catch (UnknownHostException e) {
                    Log.debug("Unknown host, network down?", e);
                } catch (IOException e) {
                    report = FAILEDSTATE + ": server problem";
                } catch (JSONException e) {
                    report = FAILEDSTATE + ": JSON parse problem";
                } catch (URISyntaxException e) {
                    report = FAILEDSTATE + ": wrong URI";
                }
                return null;
            }

            @Override
            protected void done() {
                if (!this.isCancelled()) {
                    if (report == null && result != null) {
                        parseData(result);
                        result = null;
                        if (position != null && position.length > 0) {
                            setLoaded(true);
                        } else if (position == null) {
                            report = "response is void";
                        } else {
                            report = "response is zero length array";
                        }
                    }
                    if (report != null) {
                        fireLoaded(report);
                        report = null;
                    }
                }
            }
        };
        worker.execute();
    }

    private void setLoaded(boolean isLoaded) {
        this.isLoaded = isLoaded;
        if (isLoaded) {
            this.fireLoaded(LOADEDSTATE);
        }
    }

    private void parseData(JSONObject jsonResult) {
        try {
            JSONArray resArray = jsonResult.getJSONArray("result");
            int resLength = resArray.length();
            Position.Latitudinal[] positionHelper = new Position.Latitudinal[resLength];

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
                positionHelper[j] = new Position.Latitudinal(date.getTime(), rad, lon, lat);
            }
            this.position = positionHelper;
        } catch (JSONException e) {
            this.fireLoaded(PARTIALSTATE);
            Log.warn("JSON response parse failure", e);
        } catch (ParseException e) {
            this.fireLoaded(PARTIALSTATE);
            Log.warn("JSON response parse failure", e);
        } catch (NumberFormatException e) {
            this.fireLoaded(PARTIALSTATE);
            Log.warn("JSON response parse failure", e);
        }
    }

    public boolean isLoaded() {
        return this.isLoaded;
    }

    private void applyChanges() {
        this.setLoaded(false);
        this.position = null;
        this.requestData();
    }

    public void setBeginDate(Date beginDate, boolean applyChanges) {
        this.beginDate = TimeUtils.utcFullDateFormat.format(beginDate);
        this.beginDatems = beginDate;
        if (applyChanges) {
            applyChanges();
        }
    }

    public void setEndDate(Date endDate, boolean applyChanges) {
        this.endDate = TimeUtils.utcFullDateFormat.format(endDate);
        this.endDatems = endDate;
        if (applyChanges) {
            applyChanges();
        }
    }

    public void fireLoaded(final String state) {
        camera.fireNewLoaded(state);
    }

    public Date getBeginDate() {
        return this.beginDatems;
    }

    public Date getEndDate() {
        return this.endDatems;
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
        this.observer = object;
        if (applyChanges) {
            applyChanges();
        }
    }

}
