package org.helioviewer.gl3d.camera;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;

import org.helioviewer.base.DownloadStream;
import org.helioviewer.base.logging.Log;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.jhv.display.Displayer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

public class GL3DPositionLoading {
    private final String LOADEDSTATE = "Loaded";
    private final String LOADINGSTATE = "Loading";
    private final String FAILEDSTATE = "Failed";
    private final String PARTIALSTATE = "Failed";

    private boolean isLoaded = false;
    private URL url;
    private JSONArray jsonResult;
    public GL3DPositionDateTime[] positionDateTime;
    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private final GregorianCalendar calendar = new GregorianCalendar();
    private String beginDate = "2014-07-28T00:00:00";
    private String endDate = "2014-05-30T00:00:00";
    private String target = "EARTH";
    private final String observer = "SUN";
    private final String baseUrl = "http://localhost:7789/position?";
    private final int deltat = 60 * 60 * 6; //6 hours by default
    private final ArrayList<GL3DPositionLoadingListener> listeners = new ArrayList<GL3DPositionLoadingListener>();
    private Date beginDatems;
    private Date endDatems;

    public GL3DPositionLoading() {
        /*
         * beginDatems = LayersModel.getSingletonInstance(); endDatems = new
         * Date(System.currentTimeMillis()); beginDate =
         * this.format.format(beginDatems); endDate =
         * this.format.format(endDatems); this.requestData();
         */
    }

    private void buildRequestURL() {
        try {
            url = new URL(baseUrl + "utc=" + this.beginDate + "&utc_end=" + this.endDate + "&deltat=" + deltat + "&observer=" + observer + "&target=" + target + "&ref=HEEQ&kind=latitudinal");
        } catch (MalformedURLException e) {
            Log.error("A wrong url is given.", e);
        }
    }

    public void requestData() {
        Thread loadData = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    buildRequestURL();
                    DownloadStream ds = new DownloadStream(url, 30000, 30000);
                    Reader reader = new BufferedReader(new InputStreamReader(ds.getInput(), "UTF-8"));
                    jsonResult = new JSONArray(new JSONTokener(reader));
                    parseData();
                    if (positionDateTime.length > 0) {
                        setLoaded(true);
                    }
                } catch (final IOException e1) {
                    fireLoaded(FAILEDSTATE);
                } catch (JSONException e2) {
                    fireLoaded(FAILEDSTATE);
                }

            }

        });
        loadData.run();
    }

    private void setLoaded(boolean isLoaded) {
        this.isLoaded = isLoaded;
        this.fireLoaded(this.LOADEDSTATE);
    }

    private void parseData() {
        calendar.clear();
        try {
            GL3DPositionDateTime[] positionDateTimehelper = new GL3DPositionDateTime[jsonResult.length()];
            for (int i = 0; i < jsonResult.length(); i++) {
                JSONArray ithArray = jsonResult.getJSONArray(i);
                String dateString = ithArray.get(0).toString();
                Date date = format.parse(dateString);
                calendar.setTime(date);
                JSONArray positionArray = ithArray.getJSONArray(1);
                double x = positionArray.getDouble(0);
                double y = positionArray.getDouble(1);
                double z = positionArray.getDouble(2);
                GL3DVec3d vec = new GL3DVec3d(x, y, z);
                positionDateTimehelper[i] = new GL3DPositionDateTime(calendar.getTimeInMillis(), vec);
            }
            Displayer.getSingletonInstance().render();
        } catch (JSONException e) {
            this.fireLoaded(this.PARTIALSTATE);
            Log.warn("Problem Parsing the JSON Response.", e);
        } catch (ParseException e) {
            this.fireLoaded(this.PARTIALSTATE);
            Log.warn("Problem Parsing the date in JSON Response.", e);
        }
    }

    public boolean isLoaded() {
        return this.isLoaded;
    }

    public void setBeginDate(Date beginDate) {
        this.beginDate = this.format.format(beginDate);
        this.beginDatems = beginDate;
        applyChanges();
    }

    public void applyChanges() {
        this.setLoaded(false);
        this.requestData();
    }

    public void setBeginDate(long beginDate) {
        this.beginDate = this.format.format(new Date(beginDate));
        this.beginDatems = new Date(beginDate);
        applyChanges();
    }

    public void setEndDate(Date endDate) {
        this.endDate = this.format.format(endDate);
        this.endDatems = endDate;
        applyChanges();
    }

    public void setEndDate(long endDate) {
        this.endDate = this.format.format(new Date(endDate));
        this.endDatems = new Date(endDate);
        applyChanges();
    }

    public void addListener(GL3DPositionLoadingListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void fireLoaded(String state) {
        synchronized (listeners) {
            for (GL3DPositionLoadingListener listener : listeners) {
                listener.fireNewLoaded(state);
            }
        }
    }

    public Date getBeginDate() {
        return this.beginDatems;
    }

    public Date getEndDate() {
        return this.endDatems;
    }

    public GL3DVec3d getInterpolatedPosition(long currentCameraTime) {
        long t3 = this.getBeginDate().getTime();
        long t4 = this.getEndDate().getTime();
        double interpolatedIndex = (1. * (currentCameraTime - t3) / (t4 - t3) * this.positionDateTime.length);
        int i = (int) interpolatedIndex;
        i = Math.min(i, this.positionDateTime.length - 1);
        int inext = Math.min(i + 1, this.positionDateTime.length - 1);
        double alpha = 1. - interpolatedIndex % 1.;
        double hgln = alpha * this.positionDateTime[i].getPosition().y + (1 - alpha) * this.positionDateTime[inext].getPosition().y;
        double hglt = alpha * this.positionDateTime[i].getPosition().z + (1 - alpha) * this.positionDateTime[inext].getPosition().z;
        double dist = alpha * this.positionDateTime[i].getPosition().x + (1 - alpha) * this.positionDateTime[inext].getPosition().x;
        dist = dist * 1000 / Constants.SunRadiusInMeter;
        GL3DVec3d vec = new GL3DVec3d(dist, hgln, hglt);
        return vec;
    }

    public void setObservingObject(String object) {
        this.target = object;
        this.applyChanges();
    }
}
