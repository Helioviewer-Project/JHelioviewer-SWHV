package org.helioviewer.jhv.plugins.swek.sources.comesep;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.DownloadStream;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.data.datatype.event.JHVDatabase;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.download.SWEKParam;
import org.helioviewer.jhv.plugins.swek.sources.SWEKDownloader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ComesepDownloader implements SWEKDownloader {

    /** The hek source properties */
    private final Properties comesepSourceProperties;
    private boolean overmax = false;

    /**
     * Default constructor.
     */
    public ComesepDownloader() {
        ComesepProperties csp = ComesepProperties.getSingletonInstance();
        comesepSourceProperties = csp.getComesepProperties();
    }

    @Override
    public boolean extern2db(JHVEventType eventType, Date startDate, Date endDate, List<SWEKParam> params) {
        ArrayList<Interval<Date>> range = JHVDatabase.db2daterange(eventType);
        for (Interval<Date> interval : range) {
            if (interval.getStart().getTime() <= startDate.getTime() && interval.getEnd().getTime() >= endDate.getTime()) {
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

    private boolean parseStream(InputStream stream, JHVEventType type) {
        try {
            StringBuilder sb = new StringBuilder();
            if (stream != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                JSONObject eventJSON;
                String reply = sb.toString().trim().replaceAll("[\n\r\t]", "");
                eventJSON = new JSONObject(reply);
                overmax = eventJSON.getBoolean("overmax");
                boolean success = parseEvents(eventJSON, type);
                if (!success)
                    return false;

                success = parseAssociations(eventJSON);
                return success;
            } else {
                Log.error("Download input stream was null. Probably the hek is down.");
                return false;
            }
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

    private boolean parseEvents(JSONObject eventJSON, JHVEventType type) {
        JSONArray results = eventJSON.getJSONArray("results");
        try {
            for (int i = 0; i < results.length(); i++) {
                JSONObject result = results.getJSONObject(i);

                String uid = result.getString("alertid");
                long start;
                long end;

                start = result.getLong("atearliest") * 1000;
                end = result.getLong("atlatest") * 1000;
                if (result.has("liftoffduration_value")) {
                    long cactusLiftOff = result.getLong("liftoffduration_value");
                    end = end + cactusLiftOff * 60000;
                }

                byte[] compressedJson;
                try {
                    compressedJson = JHVDatabase.compress(result.toString());
                } catch (IOException e) {
                    Log.error("compression error");
                    return false;
                }
                int id = JHVDatabase.dump_event2db(compressedJson, start, end, uid, type);
                if (id == -1) {
                    Log.error("failed to dump to database");
                    return false;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static boolean parseAssociations(JSONObject eventJSON) {
        JSONArray associations = eventJSON.getJSONArray("association");
        for (int i = 0; i < associations.length(); i++) {
            JSONObject asobj = associations.getJSONObject(i);
            Integer[] ret = JHVDatabase.dump_association2db(asobj.getString("first_ivorn"), asobj.getString("second_ivorn"));
            if (ret[0] == -1 && ret[1] == -1) {
                return false;
            }
        }
        return true;
    }

    private String createURL(SWEKEventType eventType, Date startDate, Date endDate, List<SWEKParam> params, int page) {
        StringBuilder baseURL = new StringBuilder(comesepSourceProperties.getProperty("comesepsource.baseurl")).append("?");
        baseURL = appendModel(baseURL, params).append("&");
        baseURL.append("startdate=").append(formatDate(startDate)).append("&");
        baseURL.append("enddate=").append(formatDate(endDate)).append("&");
        return baseURL.toString();
    }

    /**
     * Extracts the model from the list of parameters and appends the model to
     * the given URL.
     *
     * @param baseURL
     *            the current URL
     * @param params
     *            the list with params
     * @return
     */
    private StringBuilder appendModel(StringBuilder baseURL, List<SWEKParam> params) {
        String model = "";
        for (SWEKParam p : params) {
            if (p.param.equals("provider")) {
                model = p.value;
                break;
            }
        }
        return baseURL.append("model=").append(model);
    }

    /**
     * Formats a date in the yyyy-mm-ddThh:mm:ss format.
     *
     * @param date
     *            the date to format
     * @return the date in format yyyy-mm-ddThh:mm-ss
     */
    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return sdf.format(date);
    }

}
