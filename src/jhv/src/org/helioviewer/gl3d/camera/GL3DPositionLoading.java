package org.helioviewer.gl3d.camera;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import javax.swing.SwingWorker;

import org.helioviewer.base.DownloadStream;
import org.helioviewer.base.logging.Log;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.jhv.display.Displayer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class GL3DPositionLoading {
    private final String LOADEDSTATE = "Loaded";
    private final String FAILEDSTATE = "Failed";
    private final String PARTIALSTATE = "Partial";
    boolean running = false;

    private boolean isLoaded = false;
    private URL url;
    private JSONObject jsonResult;
    public GL3DPositionDateTime[] positionDateTime;
    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    private String beginDate = "2014-05-28T00:00:00";
    private String endDate = "2014-05-28T00:00:00";
    private final String target = "SUN";
    private String observer = "Earth";
    private final String baseUrl = "http://swhv.oma.be/position?";
    private int deltat = 45;
    private final ArrayList<GL3DPositionLoadingListener> listeners = new ArrayList<GL3DPositionLoadingListener>();
    private Date beginDatems = new Date(0);
    private Date endDatems = new Date();
    private SwingWorker<Integer, Integer> worker;

    public GL3DPositionLoading() {
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
            public boolean affect = true;

            @Override
            protected Integer doInBackground() throws Exception {
                Thread.currentThread().setName("GL3DPositionLoading--Main");
                running = true;
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
                            report = "Invalid network response.";
                        }
                    }

                } catch (UnknownHostException e) {
                    Log.debug("Unknown host, network down?", e);
                } catch (final IOException e1) {
                    report = FAILEDSTATE + ": server problem";
                } catch (JSONException e2) {
                    report = FAILEDSTATE + ": json parse problem";
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
                        if (positionDateTime != null && positionDateTime.length > 0) {
                            setLoaded(true);
                        } else if (positionDateTime == null) {
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
        GregorianCalendar calendar = new GregorianCalendar();

        calendar.clear();
        try {
            GL3DPositionDateTime[] positionDateTimehelper = new GL3DPositionDateTime[jsonResult.length()];
            Iterator<String> iteratorKeys = jsonResult.keys();
            int i = 0;
            while (iteratorKeys.hasNext()) {
                String dateString = iteratorKeys.next();
                JSONArray positionArray = (JSONArray) jsonResult.get(dateString);

                Date date = format.parse(dateString);
                calendar.setTime(date);
                double x = positionArray.getDouble(0);
                double y = Math.PI + positionArray.getDouble(1);
                if (positionArray.getDouble(1) > 0) {
                    y = -Math.PI + positionArray.getDouble(1);
                }
                double z = -positionArray.getDouble(2);
                GL3DVec3d vec = new GL3DVec3d(x, y, z);
                positionDateTimehelper[i] = new GL3DPositionDateTime(calendar.getTimeInMillis(), vec);
                i++;
            }
            this.positionDateTime = positionDateTimehelper;
            Displayer.getSingletonInstance().render();
        } catch (JSONException e) {
            this.fireLoaded(this.PARTIALSTATE);
            Log.warn("Problem Parsing the JSON Response.", e);
        } catch (ParseException e) {
            this.fireLoaded(this.PARTIALSTATE);
            Log.warn("Problem Parsing the date in JSON Response.", e);
        } catch (NumberFormatException e) {
            this.fireLoaded(this.PARTIALSTATE);
            Log.warn("Problem Parsing the date in JSON Response.", e);
        }
    }

    public boolean isLoaded() {
        return this.isLoaded;
    }

    public void setBeginDate(Date beginDate, boolean applyChanges) {
        this.beginDate = this.format.format(beginDate);
        this.beginDatems = beginDate;
        if (applyChanges) {
            applyChanges();
        }
    }

    synchronized public void applyChanges() {
        this.setLoaded(false);
        this.positionDateTime = null;
        this.requestData();
    }

    public void setBeginDate(long beginDate, boolean applyChanges) {
        this.beginDate = this.format.format(new Date(beginDate));
        this.beginDatems = new Date(beginDate);
        if (applyChanges) {
            applyChanges();
        }
    }

    public void setEndDate(Date endDate, boolean applyChanges) {
        this.endDate = this.format.format(endDate);
        this.endDatems = endDate;
        if (applyChanges) {
            applyChanges();
        }
    }

    public void setEndDate(long endDate, boolean applyChanges) {
        this.endDate = this.format.format(new Date(endDate));
        this.endDatems = new Date(endDate);
        if (applyChanges) {
            applyChanges();
        }
    }

    public void addListener(GL3DPositionLoadingListener listener) {
        listeners.add(listener);

    }

    public void fireLoaded(final String state) {
        for (GL3DPositionLoadingListener listener : listeners) {
            listener.fireNewLoaded(state);
        }
    }

    public Date getBeginDate() {
        return this.beginDatems;
    }

    public Date getEndDate() {
        return this.endDatems;
    }

    synchronized public GL3DVec3d getInterpolatedPosition(long currentCameraTime) {
        if (this.isLoaded && this.positionDateTime.length > 0) {
            long t3 = this.getBeginDate().getTime();
            long t4 = this.getEndDate().getTime();
            if (t3 == t4) {
                double hgln = this.positionDateTime[0].getPosition().y;
                double hglt = this.positionDateTime[0].getPosition().z;
                double dist = this.positionDateTime[0].getPosition().x;
                dist = dist * 1000 / Constants.SunRadiusInMeter;
                return new GL3DVec3d(dist, hgln, hglt);
            } else {
                double interpolatedIndex = (1. * (currentCameraTime - t3) / (t4 - t3) * this.positionDateTime.length);
                int i = (int) interpolatedIndex;
                i = Math.min(i, this.positionDateTime.length - 1);
                if (i < 0) {
                    i = 0;
                }
                int inext = Math.min(i + 1, this.positionDateTime.length - 1);

                double alpha = 1. - interpolatedIndex % 1.;
                double hgln = alpha * this.positionDateTime[i].getPosition().y + (1 - alpha) * this.positionDateTime[inext].getPosition().y;
                double hglt = alpha * this.positionDateTime[i].getPosition().z + (1 - alpha) * this.positionDateTime[inext].getPosition().z;
                double dist = alpha * this.positionDateTime[i].getPosition().x + (1 - alpha) * this.positionDateTime[inext].getPosition().x;
                dist = dist * 1000 / Constants.SunRadiusInMeter;
                return new GL3DVec3d(dist, hgln, hglt);
            }
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
