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
import java.util.Locale;
import java.util.TimeZone;

import org.helioviewer.base.DownloadStream;
import org.helioviewer.base.logging.Log;
import org.helioviewer.base.logging.LogSettings;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.JHVUncaughtExceptionHandler;
import org.helioviewer.jhv.io.CommandLineProcessor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class GL3DPositionLoading {
    private boolean isLoaded = false;
    private URL url;
    private JSONObject jsonResult;
    public GL3DPositionDateTime[] positionDateTime;
    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private final GregorianCalendar calendar = new GregorianCalendar();
    private String beginDate = "2017-07-28T00:00:00";
    private String endDate = "2027-05-30T00:00:00";
    private String target = "SOLAR%20ORBITER";
    private final String observer = "SUN";
    private final String baseUrl = "http://swhv:7789/multiposition?";
    private final int steps = 935;

    public GL3DPositionLoading() {
        this.requestData();
    }

    private void buildRequestURL() {
        try {
            url = new URL(baseUrl + "begin_utc=" + this.beginDate + "&end_utc=" + this.endDate + "&steps=" + steps + "&observer=" + observer + "&target=" + target + "&ref=HEEQ&kind=latitudinal");
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
                    jsonResult = new JSONObject(new JSONTokener(reader));
                    parseData();
                    isLoaded = true;
                } catch (final IOException e1) {
                    Log.warn("Error Parsing the EVE Response.", e1);
                } catch (JSONException e2) {
                    Log.warn("Error Parsing the JSON Response.", e2);
                }

            }
        });
        loadData.run();
    }

    private void parseData() {
        calendar.clear();
        try {
            JSONArray posArray = jsonResult.getJSONObject("multipositionResponse").getJSONObject("multipositionResult").getJSONArray("float");
            positionDateTime = new GL3DPositionDateTime[posArray.length()];
            for (int i = 0; i < posArray.length(); i++) {
                JSONArray ithArray = posArray.getJSONArray(i);
                String dateString = ithArray.get(0).toString();
                Date date = format.parse(dateString);
                calendar.setTime(date);
                JSONArray positionArray = ithArray.getJSONArray(1);
                double x = positionArray.getDouble(0);
                double y = positionArray.getDouble(1);
                double z = positionArray.getDouble(2);
                GL3DVec3d vec = new GL3DVec3d(x, y, z);
                positionDateTime[i] = new GL3DPositionDateTime(calendar.getTimeInMillis(), vec);
            }
        } catch (JSONException e) {
            Log.warn("Problem Parsing the JSON Response.", e);
        } catch (ParseException e) {
            Log.warn("Problem Parsing the date in JSON Response.", e);
        }
    }

    public boolean isLoaded() {
        return this.isLoaded;
    }

    public void setBeginDate(String beginDate) {
        this.beginDate = beginDate;
    }

    public void setBeginDate(Date beginDate) {
        this.beginDate = this.format.format(beginDate);
    }

    public void setBeginDate(long beginDate) {
        this.beginDate = this.format.format(new Date(beginDate));
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = this.format.format(endDate);
    }

    public void setEndDate(long endDate) {
        this.endDate = this.format.format(new Date(endDate));
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public static void main(String[] args) throws InterruptedException {
        // Uncaught runtime errors are displayed in a dialog box in addition
        JHVUncaughtExceptionHandler.setupHandlerForThread();

        // Save command line arguments
        CommandLineProcessor.setArguments(args);

        // Save current default system timezone in user.timezone
        System.setProperty("user.timezone", TimeZone.getDefault().getID());

        // Per default all times should be given in GMT
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

        // Save current default locale to user.locale
        System.setProperty("user.locale", Locale.getDefault().toString());

        // Per default, the us locale should be used
        Locale.setDefault(Locale.US);

        // init log
        LogSettings.init("/settings/log4j.initial.properties", JHVDirectory.SETTINGS.getPath() + "log4j.properties", JHVDirectory.LOGS.getPath(), CommandLineProcessor.isOptionSet("--use-existing-log-time-stamp"));
        GL3DPositionLoading positionLoading = new GL3DPositionLoading();
        for (GL3DPositionDateTime positionDateTime : positionLoading.positionDateTime) {
            System.out.println(positionDateTime);
        }

    }

    private final ArrayList<GL3DPositionLoadingListener> listeners = new ArrayList<GL3DPositionLoadingListener>();

    public void addListener(GL3DPositionLoadingListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void fireLoaded() {
        synchronized (listeners) {
            for (GL3DPositionLoadingListener listener : listeners) {
                listener.fireNewLoaded();
            }
        }
    }
}
