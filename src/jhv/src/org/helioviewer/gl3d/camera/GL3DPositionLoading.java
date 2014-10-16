package org.helioviewer.gl3d.camera;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
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
import org.json.JSONObject;
import org.json.JSONTokener;

public class GL3DPositionLoading {
    private final String LOADEDSTATE = "Loaded";
    private final String FAILEDSTATE = "Failed";
    private final String PARTIALSTATE = "Partial";

    private boolean isLoaded = false;
    private URL url;
    private JSONArray jsonResult;
    public GL3DPositionDateTime[] positionDateTime;
    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private final GregorianCalendar calendar = new GregorianCalendar();
    private String beginDate = "2014-05-28T00:00:00";
    private String endDate = "2014-05-28T00:00:00";
    private String target = "Earth";
    private final String observer = "SUN";
    private final String baseUrl = "http://swhv.oma.be/position?";
    private final int deltat = 60 * 60 / 64; //1 hours by default
    private final ArrayList<GL3DPositionLoadingListener> listeners = new ArrayList<GL3DPositionLoadingListener>();
    private Date beginDatems = new Date(0);
    private Date endDatems = new Date();

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
        long now = System.currentTimeMillis();
        Thread loadData = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    buildRequestURL();
                    if (endDatems.getTime() - beginDatems.getTime() < 1000 * 60 * 60 * 24 * 20) {
                        DownloadStream ds = new DownloadStream(url.toURI(), 30000, 30000, true);
                        Reader reader = new BufferedReader(new InputStreamReader(ds.getInput(), "UTF-8"));
                        if (!ds.getResponse400()) {
                            jsonResult = new JSONArray(new JSONTokener(reader));
                            parseData();
                            if (positionDateTime.length > 0) {
                                setLoaded(true);
                            }
                        } else {
                            JSONObject jsonObject = new JSONObject(new JSONTokener(reader));
                            if (jsonObject.has("faultstring")) {
                                String faultstring = jsonObject.getString("faultstring");
                                fireLoaded(faultstring);
                            }
                        }
                    }
                } catch (final IOException e1) {
                    Log.warn(e1);
                    e1.printStackTrace();
                    fireLoaded(FAILEDSTATE + ": server problem");
                } catch (JSONException e2) {
                    fireLoaded(FAILEDSTATE + ": json parse problem");
                } catch (URISyntaxException e) {
                    fireLoaded(FAILEDSTATE + ": wrong URI");
                }

            }

        });
        loadData.start();
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
                JSONObject ithObject = jsonResult.getJSONObject(i);
                String dateString = ithObject.getString("utc").toString();
                Date date = format.parse(dateString);
                calendar.setTime(date);
                JSONArray positionArray = ithObject.getJSONArray("val");
                double x = positionArray.getDouble(0);
                double y = Math.PI + positionArray.getDouble(1);
                if (positionArray.getDouble(1) > 0) {
                    y = -Math.PI + positionArray.getDouble(1);
                }
                double z = -positionArray.getDouble(2);
                GL3DVec3d vec = new GL3DVec3d(x, y, z);
                positionDateTimehelper[i] = new GL3DPositionDateTime(calendar.getTimeInMillis(), vec);
            }
            this.positionDateTime = positionDateTimehelper;
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
        listeners.add(listener);

    }

    public void fireLoaded(final String state) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (GL3DPositionLoadingListener listener : listeners) {
                    listener.fireNewLoaded(state);
                }
            }
        });
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
        if (t3 == t4) {
            double hgln = this.positionDateTime[0].getPosition().y;
            double hglt = this.positionDateTime[0].getPosition().z;
            double dist = this.positionDateTime[0].getPosition().x;
            dist = dist * 1000 / Constants.SunRadiusInMeter;
            GL3DVec3d vec = new GL3DVec3d(dist, hgln, hglt);
            return vec;
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
            GL3DVec3d vec = new GL3DVec3d(dist, hgln, hglt);
            return vec;
        }
    }

    public void setObserver(String object) {
        this.observer = object;
        this.applyChanges();
    }

}
