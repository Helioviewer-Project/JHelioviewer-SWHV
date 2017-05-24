package org.helioviewer.jhv.plugins.swek.config;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.swing.ImageIcon;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.base.FileUtils;
import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.data.event.SWEKEventType;
import org.helioviewer.jhv.data.event.SWEKParameter;
import org.helioviewer.jhv.data.event.SWEKParameterFilter;
import org.helioviewer.jhv.data.event.SWEKRelatedEvents;
import org.helioviewer.jhv.data.event.SWEKRelatedOn;
import org.helioviewer.jhv.data.event.SWEKSource;
import org.helioviewer.jhv.data.event.SWEKSupplier;
import org.helioviewer.jhv.database.EventDatabase;
import org.helioviewer.jhv.plugins.swek.sources.comesep.ComesepDownloader;
import org.helioviewer.jhv.plugins.swek.sources.comesep.ComesepParser;
import org.helioviewer.jhv.plugins.swek.sources.hek.HEKDownloader;
import org.helioviewer.jhv.plugins.swek.sources.hek.HEKParser;
import org.json.JSONArray;
import org.json.JSONObject;

public class SWEKConfigurationManager {

    private static final String configName = "SWEKSettings.json";
    private static final String configFile = JHVDirectory.SETTINGS.getPath() + configName;

    private static final HashMap<String, SWEKSource> sources = new HashMap<>();
    private static final HashMap<String, SWEKEventType> eventTypes = new HashMap<>();
    private static final List<SWEKEventType> orderedEventTypes = new ArrayList<>();

    public static List<SWEKEventType> loadConfig() {
        SWEKIconBank.init();
        try {
            readConfig();
        } catch (Exception e) {
            Log.error("Configuration file could not be parsed: " + e);
        }

        return orderedEventTypes;
    }

    private static void readConfig() throws Exception {
        try {
            JSONObject json = JSONUtils.getJSONFile(configFile);
            if (json.getBoolean("manually_changed")) {
                parseConfig(json);
                return;
            }
        } catch (Exception e) {
            Log.error("Local configuration file could not be parsed: " + e);
        }

        try (InputStream is = FileUtils.getResourceInputStream('/' + configName)) {
            FileUtils.save(is, new File(configFile));
        }
        parseConfig(JSONUtils.getJSONFile(configFile));
    }

    private static void parseConfig(JSONObject obj) {
        EventDatabase.config_hash = Arrays.hashCode(obj.toString().toCharArray());
        parseSources(obj);
        parseEventTypes(obj);
        SWEKEventType.setSwekRelatedEvents(parseRelatedEvents(obj));
    }

    private static void parseSources(JSONObject obj) {
        JSONArray sourcesArray = obj.getJSONArray("sources");
        for (int i = 0; i < sourcesArray.length(); i++) {
            SWEKSource source = parseSource(sourcesArray.getJSONObject(i));
            if (source != null) {
                sources.put(source.getSourceName(), source);
            }
        }
    }

    private static SWEKSource parseSource(JSONObject obj) {
        String name = parseSourceName(obj);
        switch (name) {
            case "HEK":
                return new SWEKSource(name, parseGeneralParameters(obj), new HEKParser(), new HEKDownloader());
            case "COMESEP":
                return new SWEKSource(name, parseGeneralParameters(obj), new ComesepParser(), new ComesepDownloader());
            default:
                return null;
        }
    }

    private static String parseSourceName(JSONObject obj) {
        return obj.getString("name");
    }

    private static List<SWEKParameter> parseGeneralParameters(JSONObject obj) {
        JSONArray parameterArray = obj.getJSONArray("general_parameters");
        List<SWEKParameter> parameterList = new ArrayList<>();
        for (int i = 0; i < parameterArray.length(); i++) {
            parameterList.add(parseParameter(parameterArray.getJSONObject(i)));
        }
        return parameterList;
    }

    private static void parseEventTypes(JSONObject obj) {
        JSONArray eventJSONArray = obj.getJSONArray("events_types");
        for (int i = 0; i < eventJSONArray.length(); i++) {
            SWEKEventType eventType = parseEventType(eventJSONArray.getJSONObject(i));
            eventTypes.put(eventType.getDisplayName(), eventType);
            orderedEventTypes.add(eventType);
        }
    }

    private static SWEKEventType parseEventType(JSONObject obj) {
        return new SWEKEventType(parseEventName(obj), parseSuppliers(obj), parseParameterList(obj), parseEventIcon(obj));
    }

    private static ImageIcon parseEventIcon(JSONObject obj) {
        String eventIconValue = obj.getString("icon");
        try {
            URI eventIconURI = new URI(eventIconValue);
            if (eventIconURI.getScheme().toLowerCase().equals("iconbank")) {
                return SWEKIconBank.getIcon(eventIconURI.getHost());
            } else {
                return SWEKIconBank.getIcon("Other");
            }
        } catch (URISyntaxException e) {
            Log.info("Could not parse the URI " + eventIconValue + ", null icon returned");
        }
        return null;
    }

    private static String parseEventName(JSONObject obj) {
        return obj.getString("event_name");
    }

    private static List<SWEKSupplier> parseSuppliers(JSONObject obj) {
        JSONArray suppliersArray = obj.getJSONArray("suppliers");
        List<SWEKSupplier> suppliers = new ArrayList<>();
        for (int i = 0; i < suppliersArray.length(); i++) {
            suppliers.add(parseSupplier(suppliersArray.getJSONObject(i)));
        }
        return suppliers;
    }

    private static SWEKSupplier parseSupplier(JSONObject obj) {
        return new SWEKSupplier(parseSupplierName(obj), parseSupplierDisplayName(obj), parseSupplierSource(obj), parseDbName(obj));
    }

    private static String parseSupplierName(JSONObject obj) {
        return obj.getString("supplier_name");
    }

    private static String parseSupplierDisplayName(JSONObject obj) {
        return obj.getString("supplier_display_name");
    }

    private static SWEKSource parseSupplierSource(JSONObject obj) {
        return sources.get(obj.getString("source"));
    }

    private static String parseDbName(JSONObject obj) {
        return obj.getString("db");
    }

    private static List<SWEKParameter> parseParameterList(JSONObject obj) {
        JSONArray parameterListArray = obj.getJSONArray("parameter_list");
        List<SWEKParameter> parameterList = new ArrayList<>();
        for (int i = 0; i < parameterListArray.length(); i++) {
            parameterList.add(parseParameter((JSONObject) parameterListArray.get(i)));
        }
        return parameterList;
    }

    private static SWEKParameter parseParameter(JSONObject obj) {
        return new SWEKParameter(parseParameterName(obj), parseParameterDisplayName(obj), parseParameterFilter(obj), parseDefaultVisible(obj));
    }

    private static String parseParameterName(JSONObject obj) {
        return obj.getString("parameter_name");
    }

    private static String parseParameterDisplayName(JSONObject obj) {
        return obj.getString("parameter_display_name");
    }

    private static SWEKParameterFilter parseParameterFilter(JSONObject obj) {
        JSONObject filterobj = obj.optJSONObject("filter");
        if (filterobj == null)
            return null;
        return new SWEKParameterFilter(parseFilterType(filterobj), parseMin(filterobj), parseMax(filterobj), parseStartValue(filterobj), parseStepSize(filterobj), parseUnits(filterobj), parseDbType(filterobj));
    }

    private static String parseDbType(JSONObject obj) {
        return obj.getString("dbtype");
    }

    private static String parseUnits(JSONObject obj) {
        return obj.getString("units");
    }

    private static String parseFilterType(JSONObject obj) {
        return obj.getString("filter_type");
    }

    private static double parseMin(JSONObject obj) {
        return obj.getDouble("min");
    }

    private static double parseMax(JSONObject obj) {
        return obj.getDouble("max");
    }

    private static double parseStepSize(JSONObject obj) {
        return obj.getDouble("step_size");
    }

    private static double parseStartValue(JSONObject obj) {
        return obj.getDouble("start_value");
    }

    private static boolean parseDefaultVisible(JSONObject obj) {
        return obj.getBoolean("default_visible");
    }

    private static SWEKRelatedEvents parseRelatedEvent(JSONObject obj) {
        return new SWEKRelatedEvents(parseRelatedEventName(obj), parseRelatedWith(obj), parseRelatedOnList(obj));
    }

    private static SWEKEventType parseRelatedEventName(JSONObject obj) {
        return eventTypes.get(obj.getString("event_name"));
    }

    private static SWEKEventType parseRelatedWith(JSONObject obj) {
        return eventTypes.get(obj.getString("related_with"));
    }

    private static List<SWEKRelatedEvents> parseRelatedEvents(JSONObject obj) {
        JSONArray relatedEventsArray = obj.getJSONArray("related_events");
        List<SWEKRelatedEvents> relatedEventsList = new ArrayList<>();
        for (int i = 0; i < relatedEventsArray.length(); i++) {
            relatedEventsList.add(parseRelatedEvent(relatedEventsArray.getJSONObject(i)));
        }
        return relatedEventsList;
    }

    private static List<SWEKRelatedOn> parseRelatedOnList(JSONObject obj) {
        JSONArray relatedOnArray = obj.getJSONArray("related_on");
        List<SWEKRelatedOn> relatedOnList = new ArrayList<>();
        for (int i = 0; i < relatedOnArray.length(); i++) {
            relatedOnList.add(parseRelatedOn(relatedOnArray.getJSONObject(i)));
        }
        return relatedOnList;
    }

    private static SWEKRelatedOn parseRelatedOn(JSONObject obj) {
        return new SWEKRelatedOn(parseParameterFrom(obj), parseParameterWith(obj), parseDbType(obj));
    }

    private static SWEKParameter parseParameterFrom(JSONObject obj) {
        String parameterName = obj.getString("parameter_from");
        return new SWEKParameter(parameterName, parameterName, null, false);
    }

    private static SWEKParameter parseParameterWith(JSONObject obj) {
        String parameterName = obj.getString("parameter_with");
        return new SWEKParameter(parameterName, parameterName, null, false);
    }

}
