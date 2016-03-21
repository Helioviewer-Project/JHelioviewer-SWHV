package org.helioviewer.jhv.plugins.swek.sources.hek;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.DownloadStream;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.data.datatype.event.JHVDatabase;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.download.SWEKParam;
import org.helioviewer.jhv.plugins.swek.sources.SWEKDownloader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HEKDownloader implements SWEKDownloader {
    private boolean overmax = true;

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
        JSONArray results = eventJSON.getJSONArray("result");

        for (int i = 0; i < results.length(); i++) {
            JSONObject result = results.getJSONObject(i);

            String uid = result.getString("kb_archivid");
            long start;
            long end;
            try {
                start = TimeUtils.utcDateFormat.parse(result.getString("event_starttime")).getTime();
                end = TimeUtils.utcDateFormat.parse(result.getString("event_endtime")).getTime();
            } catch (JSONException e) {
                e.printStackTrace();
                return false;
            } catch (ParseException e) {
                e.printStackTrace();
                return false;
            }

            if (end - start > 3 * 24 * 60 * 60 * 1000) {
                Log.error("Possible wrong parsing of a HEK event.");
                Log.error("Event start: " + start);
                Log.error("Event end: " + end);
                Log.error("Event JSON: ");
                Log.error(result.toString());
                return false;
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

    public void db2program() {

    }

    private static String createURL(SWEKEventType eventType, Date startDate, Date endDate, List<SWEKParam> params, int page) {
        StringBuilder baseURL = new StringBuilder(HEKSourceProperties.getSingletonInstance().getHEKSourceProperties().getProperty("heksource.baseurl")).append("?");
        baseURL.append("cmd=search&");
        baseURL.append("type=column&");
        baseURL.append("event_type=").append(HEKEventFactory.getHEKEvent(eventType.getEventName()).getAbbreviation()).append("&");
        baseURL.append("event_coordsys=").append(eventType.getCoordinateSystem()).append("&");
        baseURL.append("x1=").append(eventType.getSpatialRegion().x1).append("&");
        baseURL.append("x2=").append(eventType.getSpatialRegion().x2).append("&");
        baseURL.append("y1=").append(eventType.getSpatialRegion().y1).append("&");
        baseURL.append("y2=").append(eventType.getSpatialRegion().y2).append("&");
        baseURL.append("cosec=2&");
        baseURL.append("param0=event_starttime&op0=<=&value0=").append(TimeUtils.utcDateFormat.format(endDate)).append("&");
        baseURL = appendParams(baseURL, params);
        baseURL.append("event_starttime=").append(TimeUtils.utcDateFormat.format(startDate)).append("&");
        long max = Math.max(System.currentTimeMillis(), endDate.getTime());
        baseURL.append("event_endtime=").append(TimeUtils.utcDateFormat.format(new Date(max))).append("&");
        baseURL.append("page=").append(page);
        return baseURL.toString();
    }

    private static StringBuilder appendParams(StringBuilder baseURL, List<SWEKParam> params) {
        int paramCount = 1;

        for (SWEKParam param : params) {
            String encodedValue;
            try {
                encodedValue = URLEncoder.encode(param.getValue(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                encodedValue = param.getValue();
            }
            if (param.getParam().toLowerCase().equals("provider")) {
                baseURL.append("param").append(paramCount).append("=").append("frm_name").append("&").append("op").append(paramCount).append("=").append(param.getOperand().URLEncodedRepresentation()).append("&").append("value").append(paramCount).append("=").append(encodedValue).append("&");
            } else {
                baseURL.append("param").append(paramCount).append("=").append(param.getParam()).append("&").append("op").append(paramCount).append("=").append(param.getOperand().URLEncodedRepresentation()).append("&").append("value").append(paramCount).append("=").append(encodedValue).append("&");
            }
            paramCount++;
        }
        return baseURL;
    }
}
