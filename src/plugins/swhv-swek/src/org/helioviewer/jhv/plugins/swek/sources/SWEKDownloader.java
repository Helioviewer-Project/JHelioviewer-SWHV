package org.helioviewer.jhv.plugins.swek.sources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.DownloadStream;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKParam;
import org.helioviewer.jhv.database.JHVDatabase;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public abstract class SWEKDownloader {
    protected boolean overmax = true;

    public boolean extern2db(JHVEventType eventType, Date startDate, Date endDate, List<SWEKParam> params) {
        ArrayList<Interval<Date>> range = JHVDatabase.db2daterange(eventType);
        for (Interval<Date> interval : range) {
            if (interval.start.getTime() <= startDate.getTime() && interval.end.getTime() >= endDate.getTime()) {
                return true;
            }
        }

        try {
            int page = 0;
            boolean succes = true;
            while (overmax && succes) {
                String urlString = createURL(eventType.getEventType(), startDate, endDate, params, page);
                DownloadStream ds = new DownloadStream(new URL(urlString), JHVGlobals.getStdConnectTimeout(), JHVGlobals.getStdReadTimeout());
                succes = parseStream(ds.getInput(), eventType);
                page++;
            }
            return succes;
        } catch (MalformedURLException e) {
            Log.error("Could not create URL from given string error : " + e);
            return false;
        } catch (IOException e) {
            Log.error("Could not create input stream for given URL error : " + e);
            return false;
        }
    }

    public boolean parseStream(InputStream stream, JHVEventType type) {
        if (stream == null) {
            Log.error("Download input stream was null. Probably HEK is down.");
            return false;
        }

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            JSONObject eventJSON = new JSONObject(new JSONTokener(br));

            if (eventJSON.has("overmax"))
                overmax = eventJSON.getBoolean("overmax");
            else
                overmax = false;
            boolean success = parseEvents(eventJSON, type);
            if (!success)
                return false;

            success = parseAssociations(eventJSON);
            return success;
        } catch (IOException e) {
            overmax = false;
            Log.error("Could not read the inputstream. " + e);
            e.printStackTrace();
            return false;
        } catch (JSONException e) {
            overmax = false;
            Log.error("JSON parsing error " + e);
            e.printStackTrace();
            return false;
        }
    }

    protected abstract boolean parseEvents(JSONObject eventJSON, JHVEventType type);

    protected abstract boolean parseAssociations(JSONObject eventJSON);

    protected abstract String createURL(SWEKEventType eventType, Date startDate, Date endDate, List<SWEKParam> params, int page);
}
