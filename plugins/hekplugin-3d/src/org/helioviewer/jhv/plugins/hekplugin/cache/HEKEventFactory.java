package org.helioviewer.jhv.plugins.hekplugin.cache;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.plugins.hekplugin.settings.HEKSettings;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class to parse JSON objects and create Event objects from them.
 * <p>
 * The class follows the singleton pattern.
 * 
 * @author Malte Nuhn
 * */
public class HEKEventFactory {

    // the sole instance of this class
    private static final HEKEventFactory singletonInstance = new HEKEventFactory();

    /**
     * The private constructor to support the singleton pattern.
     * */
    private HEKEventFactory() {
    }

    /**
     * Method returns the sole instance of this class.
     * 
     * @return the only instance of this class.
     * */
    public static HEKEventFactory getSingletonInstance() {
        return singletonInstance;
    }

    /**
     * Reads a JSONObject and creates a HEKEvent object from it.
     * 
     * @param json
     *            - Object to parse
     * @param sloppy
     *            - Return a not properly initialized HEKEvent if an error
     *            occurs (instead of returning null)
     * @return - the newly parsed event, null if an error occured
     * 
     * @see #UnsupportedFormat
     * @see #HEKEventFormat
     */
    public HEKEvent parseHEK(JSONObject json, boolean sloppy) {
        HEKEvent result = new HEKEvent();

        try {
            String id = json.getString("kb_archivid");

            // for multithreading: each of these calls owns its own parser
            SimpleDateFormat hekDateFormat = new SimpleDateFormat(HEKSettings.API_DATE_FORMAT);
            Date start = hekDateFormat.parse(json.getString("event_starttime"));
            Date end = hekDateFormat.parse(json.getString("event_endtime"));

            Interval<Date> duration = new Interval<Date>(start, end);

            result.setDuration(duration);
            result.setId(id);
            result.setEventObject(json);
        } catch (ParseException e) {
            if (!sloppy) {
                Log.fatal("HEKEventFactory.ParseHEK(...) >> Could not parse HEK event: " + e.getMessage());
                result = null;
            }
        } catch (JSONException e) {
            if (!sloppy) {
                Log.fatal("HEKEventFactory.ParseHEK(...) >> Could not parse HEK event: " + e.getMessage());
                result = null;
            }
        } catch (NumberFormatException e) {
            if (!sloppy) {
                Log.fatal("HEKEventFactory.ParseHEK(...) >> Could not parse HEK event: " + e.getMessage());
                result = null;
            }
        }

        return result;

    }

    /**
     * Generates a HEKPath from the current json object, limited to the category
     * of the event
     * 
     * @param cache
     *            - HEKCache for which this path is valid
     * @param json
     *            - json object to parse
     * @return - HEKPath
     */
    public HEKPath parseCategoryPath(HEKCache cache, JSONObject json) {

        HEKPath result = null;

        try {

            String type = json.getString("event_type").toLowerCase();
            String frm = json.getString("frm_name");

            String[] path = { "HEK", type, frm };
            result = new HEKPath(cache, path);

        } catch (JSONException e) {
            Log.error("", e);
        }

        return result;

    }

    /**
     * Generates a HEKPath from the current json object
     * 
     * @param cache
     *            - HEKCache for which this path is valid
     * @param json
     *            - json object to parse
     * @return - HEKPath
     */
    public HEKPath parseEventPath(HEKCache cache, JSONObject json) {

        HEKPath result = null;

        try {

            String type = json.getString("event_type").toLowerCase();
            String frm = json.getString("frm_name");
            String title = json.getString("kb_archivid");

            if (type == null || frm == null || title == null) {
                return null;
            }

            String[] path = { "HEK", type, frm, title };
            result = new HEKPath(cache, path);

        } catch (JSONException e) {
            Log.error("", e);
        }

        return result;

    }

    /**
     * Reads the HEK json response and parses it to a Map of HEKPaths and
     * HEKEvents
     * 
     * @param cache
     *            - Cache into this request should be filled later on
     * @param json
     *            - Response to parse
     * @return Map: HEKPath - HEKEvent
     */
    @SuppressWarnings("unused")
    public HashMap<HEKPath, HEKEvent> parseEvents(JSONObject json) {
        HEKCache cache = HEKCache.getSingletonInstance();

        HashMap<HEKPath, HEKEvent> result = new HashMap<HEKPath, HEKEvent>();

        try {
            JSONArray jsonEvents = json.getJSONArray("result");

            for (int i = 0; i < jsonEvents.length(); i++) {
                JSONObject entry = jsonEvents.getJSONObject(i);

                HEKPath eventPath = parseEventPath(cache, entry);

                // Something went wrong when parsing the eventPath
                if (eventPath == null) {
                    Log.fatal("Error parsing an event: Could not parse the eventPath");
                    continue;
                }

                HEKEvent event = parseHEK(entry, false);

                if (event.getDuration() == null) {
                    Log.fatal("Event has no Duration");
                }

                event.prepareCache();

                // Something went wrong when parsing the event
                if (event == null) {
                    Log.fatal("Error parsing an event: Could parse the eventPath, but not the event: " + eventPath);
                    continue;
                }

                // set the events path
                event.setPath(eventPath);
                // and the backreference inside the eventpath
                eventPath.setObject(event);

                String lastPart = eventPath.getLastPart();

                int id_counter = 1;

                while (result.containsKey(eventPath)) {
                    id_counter++;
                    eventPath.setLastPart(lastPart + " " + id_counter);
                    Log.info("ID NOT UNIQUE " + id_counter);
                }

                result.put(eventPath, event);
            }

        } catch (JSONException e) {
            Log.error("", e);
        }

        return result;
    }

    /**
     * Reads the HEK json response and parses it to a Map of HEKPaths and
     * HEKEvents
     * 
     * @param cache
     *            - Cache into this request should be filled later on
     * @param json
     *            - Response to parse
     * @return Map: HEKPath - HEKEvent
     */
    public Vector<HEKPath> parseStructure(JSONObject json) {
        HEKCache cache = HEKCache.getSingletonInstance();

        Vector<HEKPath> result = new Vector<HEKPath>();

        try {
            JSONArray jsonEvents = json.getJSONArray("result");

            for (int i = 0; i < jsonEvents.length(); i++) {
                JSONObject entry = jsonEvents.getJSONObject(i);

                HEKPath eventPath = parseCategoryPath(cache, entry);
                result.add(eventPath);

            }

        } catch (JSONException e) {
            Log.error("", e);
        }

        return result;
    }
}
