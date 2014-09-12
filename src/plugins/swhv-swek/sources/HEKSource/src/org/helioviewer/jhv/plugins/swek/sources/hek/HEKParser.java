package org.helioviewer.jhv.plugins.swek.sources.hek;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.data.datatype.JHVEventParameter;
import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.config.SWEKParameter;
import org.helioviewer.jhv.plugins.swek.config.SWEKSource;
import org.helioviewer.jhv.plugins.swek.sources.SWEKEventStream;
import org.helioviewer.jhv.plugins.swek.sources.SWEKParser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Parser able to parse events coming from the HEK server.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class HEKParser implements SWEKParser {

    /** Is the parser stopped */
    private boolean parserStopped;

    /** The hek event stream */
    private final HEKEventStream eventStream;

    /** The event type for this parser */
    private SWEKEventType eventType;

    /** the event source for this parser */
    private SWEKSource eventSource;

    /**
     * Creates a parser for the given event type and event source.
     * 
     * @param eventType
     *            the type of the event
     * @param source
     *            the source of the event
     * 
     */
    public HEKParser() {
        parserStopped = false;
        eventStream = new HEKEventStream();
    }

    @Override
    public void stopParser() {
        parserStopped = true;
    }

    @Override
    public SWEKEventStream parseEventStream(InputStream downloadInputStream, SWEKEventType eventType, SWEKSource eventSource) {
        this.eventType = eventType;
        this.eventSource = eventSource;
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(downloadInputStream));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            JSONObject eventJSON;
            eventJSON = new JSONObject(sb.toString());
            parseEventJSON(eventJSON);
            return eventStream;
        } catch (IOException e) {
            Log.error("Could not read the inputstream. " + e);
            e.printStackTrace();
        } catch (JSONException e) {
            Log.error("Could not create the JSON object from stream. " + e);
            e.printStackTrace();
        }
        return eventStream;
    }

    /**
     * Parses the event JSON returned by the server.
     * 
     * @param eventJSON
     *            the JSON object
     * @throws JSONException
     *             if the json object could not be parsed
     */
    private void parseEventJSON(JSONObject eventJSON) throws JSONException {
        JSONArray results = eventJSON.getJSONArray("result");
        for (int i = 0; i < results.length() && !parserStopped; i++) {
            HEKEvent currentEvent = new HEKEvent(eventType.getEventName(), eventType.getEventName(), "");
            JSONObject result = results.getJSONObject(i);
            parseResult(result, currentEvent);
            eventStream.addJHVEvent(currentEvent);
        }
    }

    /**
     * Parses one result returned by the HEK server.
     * 
     * @param result
     *            the result to be parsed.
     * @param currentEvent
     *            the current event the is parsed
     * @throws JSONException
     *             if the result could not be parsed
     */
    private void parseResult(JSONObject result, HEKEvent currentEvent) throws JSONException {
        Iterator<?> keys = result.keys();
        while (keys.hasNext()) {
            parseParameter(result, keys.next(), currentEvent);
        }
    }

    /**
     * Parses the parameter
     * 
     * @param result
     *            the result from where to parse the parameter
     * @param key
     *            the key in the json
     * @param currentEvent
     *            the event currently parsed
     * @throws JSONException
     *             if the parameter could not be parsed
     */
    private void parseParameter(JSONObject result, Object key, HEKEvent currentEvent) throws JSONException {
        if (key instanceof String) {
            String keyString = (String) key;
            String value = result.getString((String) key);
            if (keyString.toLowerCase().equals("event_starttime")) {
                currentEvent.setStartTime(parseDate(value));
            } else if (keyString.toLowerCase().equals("event_endtime")) {
                currentEvent.setEndTime(parseDate(value));
            } else {

                boolean visible = false;
                boolean configured = false;
                JHVEventParameter parameter = new JHVEventParameter(keyString, keyString, value);
                if (!eventType.containsParameter(keyString)) {
                    if (eventSource.containsParameter(keyString)) {
                        configured = true;
                        SWEKParameter p = eventSource.getParameter(keyString);
                        if (p != null) {
                            visible = p.isDefaultVisible();
                        }
                    }
                } else {
                    configured = true;
                    SWEKParameter p = eventType.getParameter(keyString);
                    if (p != null) {
                        visible = p.isDefaultVisible();
                    }
                }
                currentEvent.addParameter(parameter, visible, configured);
            }
        }
    }

    private Date parseDate(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        try {
            return sdf.parse(date);
        } catch (ParseException e) {
            Log.error("The date " + date + " could not be parsed.");
            return null;
        }
    }
}
