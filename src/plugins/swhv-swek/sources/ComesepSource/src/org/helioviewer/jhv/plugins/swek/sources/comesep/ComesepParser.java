package org.helioviewer.jhv.plugins.swek.sources.comesep;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.data.datatype.event.JHVAssociation;
import org.helioviewer.jhv.data.datatype.event.JHVDatabase;
import org.helioviewer.jhv.data.datatype.event.JHVEventParameter;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKParameter;
import org.helioviewer.jhv.data.datatype.event.SWEKParser;
import org.helioviewer.jhv.data.datatype.event.SWEKRelatedEvents;
import org.helioviewer.jhv.data.datatype.event.SWEKSource;
import org.helioviewer.jhv.data.datatype.event.SWEKSupplier;
import org.helioviewer.jhv.plugins.swek.sources.SWEKEventStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ComesepParser implements SWEKParser {

    private SWEKEventType eventType;
    private SWEKSource eventSource;
    private SWEKSupplier eventSupplier;
    private boolean parserStopped;
    private Long cactusLiftOff = null;
    private boolean startTimeSet = false;
    private boolean endTimeSet = false;

    /**
     * Creates a parser for the given event type and event source.
     *
     * @param eventType
     *            the type of the event
     * @param source
     *            the source of the event
     *
     */
    public ComesepParser() {
        parserStopped = false;
    }

    public void stopParser() {
        parserStopped = true;
    }

    public SWEKEventStream parseEventStream(InputStream downloadInputStream, SWEKEventType eventType, SWEKSource swekSource, SWEKSupplier swekSupplier, List<SWEKRelatedEvents> relatedEvents, boolean todb) {
        final ComesepEventStream eventStream = new ComesepEventStream();

        this.eventType = eventType;
        eventSource = swekSource;
        eventSupplier = swekSupplier;
        try {
            StringBuilder sb = new StringBuilder();
            if (downloadInputStream != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(downloadInputStream, "UTF-8"));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                JSONObject eventJSON;
                String reply = sb.toString().trim().replaceAll("[\n\r\t]", "");
                eventJSON = new JSONObject(reply);
                parseEventJSON(eventJSON, eventStream);
                parseAssociation(eventJSON, eventStream);
                return eventStream;
            } else {
                Log.error("Download input stream was null. Probably the comesep server is down.");
            }
        } catch (IOException e) {
            Log.error("Could not read the inputstream. " + e);
            e.printStackTrace();
        } catch (JSONException e) {
            Log.error("Could not create the JSON object from stream. " + e);
            e.printStackTrace();
        }
        return eventStream;
    }

    private void parseEventJSON(JSONObject eventJSON, ComesepEventStream eventStream) throws JSONException {
        JSONArray results = eventJSON.getJSONArray("results");
        JHVEventType comesepEventType = JHVEventType.getJHVEventType(eventType, eventSupplier);
        for (int i = 0; i < results.length() && !parserStopped; i++) {
            ComesepEvent currentEvent = new ComesepEvent(eventType.getEventName(), eventType.getEventName(), comesepEventType, eventType.getEventIcon(), eventType.getColor());
            JSONObject result = results.getJSONObject(i);
            parseResult(result, currentEvent);
            initLocalVariables();
            eventStream.addJHVEvent(currentEvent);
        }
    }

    private void initLocalVariables() {
        startTimeSet = false;
        endTimeSet = false;
        cactusLiftOff = null;
    }

    private void parseResult(JSONObject result, ComesepEvent currentEvent) throws JSONException {
        Iterator<?> keys = result.keys();
        while (keys.hasNext()) {
            parseParameter(result, keys.next(), currentEvent);
        }
    }

    private void parseParameter(JSONObject result, Object key, ComesepEvent currentEvent) throws JSONException {
        if (key instanceof String) {
            String keyString = (String) key;

            String value = null;
            if (!result.isNull(keyString))
                value = result.optString(keyString); // convert to string
            else
                return;

            // Event start time
            if (keyString.toLowerCase().equals("atearliest")) {
                startTimeSet = true;
                currentEvent.setStartTime(parseDate(value));
                if (cactusLiftOff != null) {
                    currentEvent.setEndTime(new Date(currentEvent.getStartDate().getTime() + cactusLiftOff * 60000));
                    endTimeSet = true;
                }
            } else
            // Event end time
            if (keyString.toLowerCase().equals("atlatest")) {
                if (!endTimeSet) {
                    currentEvent.setEndTime(parseDate(value));
                    endTimeSet = true;
                }
            } else
            // event unique ID
            if (keyString.toLowerCase().equals("alertid")) {
                currentEvent.setUniqueID(value.hashCode());
            } else if (keyString.toLowerCase().equals("liftoffduration_value")) {
                cactusLiftOff = Long.valueOf(value);
                if (startTimeSet) {
                    currentEvent.setEndTime(new Date(currentEvent.getStartDate().getTime() + cactusLiftOff * 60000));
                    endTimeSet = true;
                }
            } else if (keyString.toLowerCase().equals("begin_time_value")) {
                currentEvent.setStartTime(new Date(Long.parseLong(value) * 1000));
                startTimeSet = true;
            } else if (keyString.toLowerCase().equals("end_time_value")) {
                currentEvent.setStartTime(new Date(Long.parseLong(value) * 1000));
                endTimeSet = true;
            } else {
                boolean visible = false;
                boolean configured = false;
                String displayName = keyString;
                if (eventType.containsParameter(keyString) || eventSource.containsParameter(keyString)) {
                    configured = true;
                    SWEKParameter p = eventSource.getParameter(keyString);
                    if (p != null) {
                        visible = p.isDefaultVisible();
                        displayName = p.getParameterDisplayName();
                    } else {
                        displayName = keyString.replaceAll("_", " ").trim();
                    }
                }
                JHVEventParameter parameter = new JHVEventParameter(keyString, displayName, value);

                currentEvent.addParameter(parameter, visible, configured);
            }
        }
    }

    private Date parseDate(String value) {
        return new Date(Long.parseLong(value) * 1000);
    }

    private void parseAssociation(JSONObject eventJSON, ComesepEventStream eventStream) throws JSONException {
        JSONArray associations = eventJSON.getJSONArray("associations");
        for (int i = 0; i < associations.length() && !parserStopped; i++) {
            Integer[] idlist = JHVDatabase.dump_association2db(parseAssociationChild(associations.getJSONObject(i)), parseAssociationParent(associations.getJSONObject(i)));
            JHVAssociation association = new JHVAssociation(idlist[0], idlist[1]);
            if (idlist[0] != -1 && idlist[1] != -1)
                eventStream.addJHVAssociation(association);
        }
    }

    private String parseAssociationParent(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("parent");
    }

    private String parseAssociationChild(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("child");
    }

    @Override
    public boolean parseEventJSON(String json, JHVEventType type, int id, long start, long end) throws JSONException {
        // TODO Auto-generated method stub
        return false;
    }

}
