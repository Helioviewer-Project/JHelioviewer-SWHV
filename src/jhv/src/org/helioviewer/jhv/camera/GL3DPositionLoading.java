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
import java.util.List;

import javax.swing.SwingWorker;

import org.helioviewer.base.DownloadStream;
import org.helioviewer.base.astronomy.Position;
import org.helioviewer.base.astronomy.Sun;
import org.helioviewer.base.datetime.TimeUtils;
import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.GL3DVec3d;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class GL3DPositionLoading {

    private final String LOADEDSTATE = "Loaded";
    private final String FAILEDSTATE = "Failed";
    private final String PARTIALSTATE = "Partial";

    private boolean isLoaded = false;
    private URL url;
    private JSONObject jsonResult;
    private Position.Latitudinal[] position;
    private String beginDate = "2014-05-28T00:00:00";
    private String endDate = "2014-05-28T00:00:00";
    private final String target = "SUN";
    private String observer = "Earth";
    private final String baseUrl = "http://swhv.oma.be/position?";
    private int deltat = 45;
    private Date beginDatems = new Date(0);
    private Date endDatems = new Date();
    private SwingWorker<Integer, Integer> worker;
    private final GL3DExpertCamera camera;

    public GL3DPositionLoading(GL3DExpertCamera camera) {
        this.camera = camera;
    }

    private void buildRequestURL() {
        try {
            url = new URL(baseUrl + "abcorr=LT%2BS&utc=" + this.beginDate + "&utc_end=" + this.endDate + "&deltat=" + deltat + "&observer=" + observer + "&target=" + target + "&ref=HEEQ&kind=latitudinal");
        } catch (MalformedURLException e) {
            Log.error("A wrong url is given.", e);
        }
    }

    public void requestData() {
        if (worker != null) {
            worker.cancel(false);
        }
        fireLoaded("Loading...");

        worker = new SwingWorker<Integer, Integer>() {
            private String report = null;

            @Override
            protected Integer doInBackground() throws Exception {
                Thread.currentThread().setName("GL3DPositionLoading--Main");
                try {
                    if (endDatems.getTime() - beginDatems.getTime() < 1000 * 60 * 60 * 24 * 20) {
                        deltat = 60 * 60 / 64;
                    } else {
                        deltat = 60 * 60 * 24;
                    }
                    buildRequestURL();

                    DownloadStream ds = new DownloadStream(url.toURI(), 30000, 30000, true);
                    Reader reader = new BufferedReader(new InputStreamReader(ds.getInput(), "UTF-8"));
                    if (!ds.getResponse400()) {
                        jsonResult = new JSONObject(new JSONTokener(reader));
                    } else {
                        JSONObject jsonObject = new JSONObject(new JSONTokener(reader));
                        if (jsonObject.has("faultstring")) {
                            String faultstring = jsonObject.getString("faultstring");
                            report = faultstring;
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

                return 1;
            }

            @Override
            public void process(List<Integer> chunks) {
            }

            @Override
            public void done() {
                if (!this.isCancelled()) {
                    if (report == null && jsonResult != null) {
                        parseData();
                        jsonResult = null;
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
                    }
                }
            }
        };
        worker.execute();
    }

    private void setLoaded(boolean isLoaded) {
        this.isLoaded = isLoaded;
        if (isLoaded) {
            this.fireLoaded(this.LOADEDSTATE);
        }
    }

    private void parseData() {
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
            this.fireLoaded(this.PARTIALSTATE);
            Log.warn("JSON response parse failure", e);
        } catch (ParseException e) {
            this.fireLoaded(this.PARTIALSTATE);
            Log.warn("JSON response parse failure", e);
        } catch (NumberFormatException e) {
            this.fireLoaded(this.PARTIALSTATE);
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
        int llen = position.length;
        if (llen > 0) {
            return position[0].milli;
        }
        return -1L;
    }

    public long getEndTime() {
        int llen = position.length;
        if (llen > 0) {
            return position[llen - 1].milli;
        }
        return -1L;
    }

    public GL3DVec3d getInterpolatedPosition(long currentCameraTime) {
        if (this.isLoaded && position.length > 0) {
            double dist, hgln, hglt;

            long tstart = getStartTime();
            long tend = getEndTime();
            if (tstart == tend) {
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
                double alpha = 1. - interpolatedIndex % 1.;
                dist = alpha * position[i].rad + (1. - alpha) * position[inext].rad;
                hgln = alpha * position[i].lon + (1. - alpha) * position[inext].lon;
                hglt = alpha * position[i].lat + (1. - alpha) * position[inext].lat;
            }
            return new GL3DVec3d(dist, hgln, hglt);
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
