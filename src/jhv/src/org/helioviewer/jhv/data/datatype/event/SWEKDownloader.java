package org.helioviewer.jhv.data.datatype.event;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.base.DownloadStream;
import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.database.EventDatabase;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class SWEKDownloader {

    public boolean extern2db(JHVEventType eventType, long start, long end, List<SWEKParam> params) {
        ArrayList<Interval> range = EventDatabase.db2daterange(eventType);
        for (Interval interval : range) {
            if (interval.start <= start && interval.end >= end) {
                return true;
            }
        }
        try {
            int page = 0;
            boolean success = true;
            boolean overmax = true;

            while (overmax && success) {
                String urlString = createURL(eventType.getEventType(), start, end, params, page);
                InputStream stream = new DownloadStream(new URL(urlString)).getInput();
                if (stream == null) {
                    Log.error("Download input stream was null. Probably HEK is down.");
                    return false;
                }
                JSONObject eventJSON = JSONUtils.getJSONStream(stream);
                overmax = getOvermax(eventJSON);
                success = parseEvents(eventJSON, eventType) && parseAssociations(eventJSON);
                page++;
            }
            return success;
        } catch (MalformedURLException e) {
            Log.error("Could not create URL from given string error : " + e);
        } catch (IOException e) {
            Log.error("Could not create input stream for given URL error : " + e);
        }
        return false;
    }

    private boolean getOvermax(JSONObject eventJSON) {
        boolean overmax = true;
        try {
            overmax = eventJSON.has("overmax") && eventJSON.getBoolean("overmax");
        } catch (JSONException e) {
            overmax = false;
            Log.error("JSON parsing error " + e);
            e.printStackTrace();
            return false;
        }
        return overmax;
    }

    protected abstract boolean parseEvents(JSONObject eventJSON, JHVEventType type);

    protected abstract boolean parseAssociations(JSONObject eventJSON);

    protected abstract String createURL(SWEKEventType eventType, long start, long end, List<SWEKParam> params, int page);

}
