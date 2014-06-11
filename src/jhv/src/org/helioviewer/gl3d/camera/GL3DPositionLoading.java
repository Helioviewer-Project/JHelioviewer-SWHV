package org.helioviewer.gl3d.camera;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
    private boolean isLoaded;
    private URL url;
    private JSONObject jsonResult;
    public GL3DPositionDateTime[] positionDateTime;

    public GL3DPositionLoading() {
        buildRequestURL();
        this.requestData();
        this.parseData();
    }

    private void buildRequestURL() {
        try {
            url = new URL("http://swhv:7789/multiposition?begin_utc=2017-07-28T00:00:00&end_utc=2019-05-30T00:00:00&steps=48&observer=SUN&target=SOLAR%20ORBITER&ref=HEEQ&kind=latitudinal");
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void requestData() {
        Thread loadData = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DownloadStream ds = new DownloadStream(url, 30000, 30000);
                    Reader reader = new BufferedReader(new InputStreamReader(ds.getInput(), "UTF-8"));
                    jsonResult = new JSONObject(new JSONTokener(reader));
                    isLoaded = true;
                } catch (final IOException e1) {
                    Log.error("Error Parsing the EVE Response.", e1);
                } catch (JSONException e2) {
                    Log.error("Error Parsing the JSON Response.", e2);
                }

            }
        });
        loadData.run();
    }

    private void parseData() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        final GregorianCalendar calendar = new GregorianCalendar();
        calendar.clear();
        try {
            JSONArray posArray = this.jsonResult.getJSONObject("multipositionResponse").getJSONObject("multipositionResult").getJSONArray("float");
            this.positionDateTime = new GL3DPositionDateTime[posArray.length()];
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
                this.positionDateTime[i] = new GL3DPositionDateTime(calendar.getTimeInMillis(), vec);
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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
}
