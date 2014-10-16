package org.helioviewer.gl3d.spaceobjects;

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
import org.helioviewer.gl3d.camera.GL3DPositionDateTime;
import org.helioviewer.gl3d.camera.GL3DPositionLoadingListener;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.jhv.display.Displayer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class GL3DPositionLoadingPlanet {
    private final String LOADEDSTATE = "Loaded";
    private final String FAILEDSTATE = "Failed";
    private final String PARTIALSTATE = "Partial";

    private boolean isLoaded = false;
    private URL url;
    private JSONArray jsonResult;
    public GL3DPositionDateTime[] positionDateTime;
    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private final GregorianCalendar calendar = new GregorianCalendar();
    private String beginDate = "";
    private String endDate = "";
    private String target = "Earth";
    private String observer = "SUN";
    private final String baseUrl = "http://127.0.0.1:7789/position?";
    private final int deltat = 60 * 60 / 32; //1 hours by default
    private final ArrayList<GL3DPositionLoadingListener> listeners = new ArrayList<GL3DPositionLoadingListener>();
    private Date beginDatems = new Date();
    private Date endDatems = new Date(0);

    public GL3DPositionLoadingPlanet() {
    }

    private void buildRequestURL() {
        try {
            if (this.endDatems.getTime() - this.beginDatems.getTime() < 1000 * 60 * 60 * 24 * 10) {
                url = new URL(baseUrl + "abcorr=LT%2BS&utc=" + this.beginDate + "&utc_end=" + this.endDate + "&deltat=" + deltat + "&observer=" + observer + "&target=" + target + "&ref=HEEQ");
            } else {
                url = new URL("");
            }
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
                    System.out.println("PLANETLOAD" + url);

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
                JSONObject ithObject = jsonResult.getJSONObject(i);
                String dateString = ithObject.getString("utc").toString();
                Date date = format.parse(dateString);
                calendar.setTime(date);
                JSONArray positionArray = ithObject.getJSONArray("val");
                double x = positionArray.getDouble(0);
                double y = positionArray.getDouble(1);
                double z = positionArray.getDouble(2);
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
        this.beginDate = this.format.format(new Date(beginDate.getTime() - 1000 * 60 * 60 * 24));
        this.beginDatems = new Date(beginDate.getTime() - 1000 * 60 * 60 * 24);
        System.out.println(beginDate);
        applyChanges();
    }

    public void applyChanges() {
        this.setLoaded(false);
        this.requestData();
    }

    public void setBeginDate(long beginDate) {
        this.beginDate = this.format.format(new Date(beginDate - 1000 * 60 * 60 * 24));
        this.beginDatems = new Date(beginDate - 1000 * 60 * 60 * 24);
        applyChanges();
    }

    public void setEndDate(Date endDate) {
        this.endDate = this.format.format(new Date(endDate.getTime() + 1000 * 60 * 60 * 24));
        this.endDatems = new Date(endDate.getTime() + 1000 * 60 * 60 * 24);
        System.out.println("ENDDATE" + endDate);

        applyChanges();
    }

    public void setEndDate(long endDate) {
        this.endDate = this.format.format(new Date(endDate + 1000 * 60 * 60 * 24));
        this.endDatems = new Date(endDate + 1000 * 60 * 60 * 24);
        System.out.println("ENDDATE" + endDate);

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
            double x = this.positionDateTime[0].getPosition().x * 1000 / Constants.SunRadiusInMeter;
            double y = this.positionDateTime[0].getPosition().y * 1000 / Constants.SunRadiusInMeter;
            double z = this.positionDateTime[0].getPosition().z * 1000 / Constants.SunRadiusInMeter;
            GL3DVec3d vec = new GL3DVec3d(-y, -z, -x);
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
            double x = (alpha * this.positionDateTime[i].getPosition().x + (1 - alpha) * this.positionDateTime[inext].getPosition().x) * 1000 / Constants.SunRadiusInMeter;
            double y = (alpha * this.positionDateTime[i].getPosition().y + (1 - alpha) * this.positionDateTime[inext].getPosition().y) * 1000 / Constants.SunRadiusInMeter;
            double z = (alpha * this.positionDateTime[i].getPosition().z + (1 - alpha) * this.positionDateTime[inext].getPosition().z) * 1000 / Constants.SunRadiusInMeter;
            GL3DVec3d vec = new GL3DVec3d(-y, -z, -x);
            return vec;
        }
    }

    public void setTarget(String object) {
        this.target = object;
    }

    public void setObserver(String object) {
        this.observer = object;
    }
}
