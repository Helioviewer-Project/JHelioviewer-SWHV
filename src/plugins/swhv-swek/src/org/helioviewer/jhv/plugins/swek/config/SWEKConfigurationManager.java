package org.helioviewer.jhv.plugins.swek.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.FileUtils;
import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.data.datatype.event.SWEKEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKParameter;
import org.helioviewer.jhv.data.datatype.event.SWEKParameterFilter;
import org.helioviewer.jhv.data.datatype.event.SWEKRelatedEvents;
import org.helioviewer.jhv.data.datatype.event.SWEKRelatedOn;
import org.helioviewer.jhv.data.datatype.event.SWEKSource;
import org.helioviewer.jhv.data.datatype.event.SWEKSupplier;
import org.helioviewer.jhv.database.EventDatabase;
import org.helioviewer.jhv.plugins.swek.SWEKPlugin;
import org.helioviewer.jhv.plugins.swek.sources.comesep.ComesepDownloader;
import org.helioviewer.jhv.plugins.swek.sources.comesep.ComesepParser;
import org.helioviewer.jhv.plugins.swek.sources.hek.HEKDownloader;
import org.helioviewer.jhv.plugins.swek.sources.hek.HEKParser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SWEKConfigurationManager {

    private static final String configFileName = "SWEKSettings.json";

    private static boolean configLoaded = false;

    private static URL configFileURL;

    private static final Map<String, SWEKSource> sources = new HashMap<>();
    private static final Map<String, SWEKEventType> eventTypes = new HashMap<>();
    private static final List<SWEKEventType> orderedEventTypes = new ArrayList<>();

    public static List<SWEKEventType> loadConfiguration() {
        if (!configLoaded) {
            SWEKIconBank.init();

            Log.debug("search and open the configuration file");
            boolean isConfigParsed;
            if (checkAndOpenUserSetFile()) {
                isConfigParsed = parseConfigFile();
            } else if (checkAndOpenHomeDirectoryFile()) {
                // check if the file is manually changed; if not, we download the latest version anyway
                isConfigParsed = isManuallyChanged() ? parseConfigFile() : checkAndOpenZippedFile() && parseConfigFile();
            } else {
                isConfigParsed = checkAndOpenZippedFile() && parseConfigFile();
            }
            configLoaded = isConfigParsed;
        }
        return orderedEventTypes;
    }

    private static boolean isManuallyChanged() {
        try {
            Log.debug("configURL: " + configFileURL);
            JSONObject configJSON = JSONUtils.getJSONStream(configFileURL.openStream());
            return parseManuallyChanged(configJSON);
        } catch (JSONException e) {
            Log.error("Could not parse JSON: " + e.getMessage());
        } catch (IOException e) {
            Log.error("Could not load the file: " + e.getMessage());
        }
        return false;
    }

    private static boolean checkAndOpenZippedFile() {
        try (InputStream is = SWEKPlugin.class.getResourceAsStream('/' + configFileName)) {
            File f = new File(JHVDirectory.SETTINGS.getPath() + configFileName);
            FileUtils.save(is, f);
            configFileURL = f.toURI().toURL();
            return true;
        } catch (IOException e) {
            Log.debug("Something went wrong extracting the configuration file from the jar bundle or saving it: " + e);
        }
        return false;
    }

    private static boolean checkAndOpenHomeDirectoryFile() {
        String configFile = JHVDirectory.SETTINGS.getPath() + configFileName;
        try {
            File f = new File(configFile);
            if (f.exists()) {
                configFileURL = f.toURI().toURL();
                return true;
            } else {
                Log.debug("File created from the settings: " + configFile + " does not exists on this system");
            }
        } catch (MalformedURLException e) {
            Log.debug("File " + configFile + " could not be parsed into an URL");
        }
        return false;
    }

    private static boolean checkAndOpenUserSetFile() {
        Log.debug("Search for a user defined configuration file in the JHelioviewer setting file");
        Settings jhvSettings = Settings.getSingletonInstance();
        String fileName = jhvSettings.getProperty("plugin.swek.configfile");
        if (fileName == null) {
            Log.debug("No configured filename found");
            return false;
        } else {
            try {
                URI fileLocation = new URI(fileName);
                configFileURL = fileLocation.toURL();
                Log.debug("Config file: " + configFileURL);
                return true;
            } catch (URISyntaxException e) {
                Log.debug("Wrong URI syntax for the found file name: " + fileName);
            } catch (MalformedURLException e) {
                Log.debug("Could not convert the URI in a correct URL. The found file name: " + fileName);
            }
            return false;
        }
    }

    private static boolean parseConfigFile() {
        try {
            JSONObject configJSON = JSONUtils.getJSONStream(configFileURL.openStream());
            EventDatabase.config_hash = Arrays.hashCode(configJSON.toString().toCharArray());

            parseSources(configJSON);
            parseEventTypes(configJSON);

            SWEKEventType.setSwekRelatedEvents(parseRelatedEvents(configJSON));
            return true;
        } catch (IOException e) {
            Log.debug("Configuration file could not be parsed: " + e);
        } catch (JSONException e) {
            Log.debug("Could not parse config JSON: " + e);
        }
        return false;
    }

    private static boolean parseManuallyChanged(JSONObject configJSON) throws JSONException {
        return configJSON.getBoolean("manually_changed");
    }

    private static void parseSources(JSONObject configJSON) throws JSONException {
        JSONArray sourcesArray = configJSON.getJSONArray("sources");
        for (int i = 0; i < sourcesArray.length(); i++) {
            SWEKSource source = parseSource(sourcesArray.getJSONObject(i));
            if (source != null) {
                sources.put(source.getSourceName(), source);
            }
        }
    }

    private static SWEKSource parseSource(JSONObject jsonObject) throws JSONException {
        String name = parseSourceName(jsonObject);
        switch (name) {
            case "HEK":
                return new SWEKSource(name, parseGeneralParameters(jsonObject), new HEKParser(), new HEKDownloader());
            case "COMESEP":
                return new SWEKSource(name, parseGeneralParameters(jsonObject), new ComesepParser(), new ComesepDownloader());
            default:
                return null;
        }
    }

    private static String parseSourceName(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("name");
    }

    private static List<SWEKParameter> parseGeneralParameters(JSONObject jsonObject) throws JSONException {
        List<SWEKParameter> parameterList = new ArrayList<>();
        JSONArray parameterArray = jsonObject.getJSONArray("general_parameters");
        for (int i = 0; i < parameterArray.length(); i++) {
            parameterList.add(parseParameter(parameterArray.getJSONObject(i)));
        }
        return parameterList;
    }

    private static void parseEventTypes(JSONObject configJSON) throws JSONException {
        JSONArray eventJSONArray = configJSON.getJSONArray("events_types");
        for (int i = 0; i < eventJSONArray.length(); i++) {
            SWEKEventType eventType = parseEventType(eventJSONArray.getJSONObject(i));
            eventTypes.put(eventType.getEventName(), eventType);
            orderedEventTypes.add(eventType);
        }
    }

    private static SWEKEventType parseEventType(JSONObject object) throws JSONException {
        return new SWEKEventType(parseEventName(object), parseSuppliers(object), parseParameterList(object), parseEventIcon(object));
    }

    private static ImageIcon parseEventIcon(JSONObject object) throws JSONException {
        String eventIconValue = object.getString("icon");
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

    private static String parseEventName(JSONObject object) throws JSONException {
        return object.getString("event_name");
    }

    private static List<SWEKSupplier> parseSuppliers(JSONObject object) throws JSONException {
        List<SWEKSupplier> suppliers = new ArrayList<>();
        JSONArray suppliersArray = object.getJSONArray("suppliers");
        for (int i = 0; i < suppliersArray.length(); i++) {
            suppliers.add(parseSupplier(suppliersArray.getJSONObject(i)));
        }
        return suppliers;
    }

    private static SWEKSupplier parseSupplier(JSONObject object) throws JSONException {
        return new SWEKSupplier(parseSupplierName(object), parseSupplierDisplayName(object), parseSupplierSource(object), parseDbName(object));
    }

    private static String parseSupplierName(JSONObject object) throws JSONException {
        return object.getString("supplier_name");
    }

    private static String parseSupplierDisplayName(JSONObject object) throws JSONException {
        return object.getString("supplier_display_name");
    }

    private static SWEKSource parseSupplierSource(JSONObject object) throws JSONException {
        return sources.get(object.getString("source"));
    }

    private static String parseDbName(JSONObject object) throws JSONException {
        return object.getString("db");
    }

    private static List<SWEKParameter> parseParameterList(JSONObject object) throws JSONException {
        List<SWEKParameter> parameterList = new ArrayList<>();
        JSONArray parameterListArray = object.getJSONArray("parameter_list");
        for (int i = 0; i < parameterListArray.length(); i++) {
            parameterList.add(parseParameter((JSONObject) parameterListArray.get(i)));
        }
        return parameterList;
    }

    private static SWEKParameter parseParameter(JSONObject jsonObject) throws JSONException {
        return new SWEKParameter(parseParameterName(jsonObject), parseParameterDisplayName(jsonObject), parseParameterFilter(jsonObject), parseDefaultVisible(jsonObject));
    }

    private static String parseParameterName(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("parameter_name");
    }

    private static String parseParameterDisplayName(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("parameter_display_name");
    }

    private static SWEKParameterFilter parseParameterFilter(JSONObject jsonObject) throws JSONException {
        JSONObject filterobject = jsonObject.optJSONObject("filter");
        if (filterobject == null)
            return null;
        return new SWEKParameterFilter(parseFilterType(filterobject), parseMin(filterobject), parseMax(filterobject), parseStartValue(filterobject), parseStepSize(filterobject), parseUnits(filterobject), parseDbType(filterobject));
    }

    private static String parseDbType(JSONObject filterobject) throws JSONException {
        return filterobject.getString("dbtype");
    }

    private static String parseUnits(JSONObject filterobject) throws JSONException {
        return filterobject.getString("units");
    }

    private static String parseFilterType(JSONObject object) throws JSONException {
        return object.getString("filter_type");
    }

    private static double parseMin(JSONObject object) throws JSONException {
        return object.getDouble("min");
    }

    private static double parseMax(JSONObject object) throws JSONException {
        return object.getDouble("max");
    }

    private static double parseStepSize(JSONObject object) throws JSONException {
        return object.getDouble("step_size");
    }

    private static Double parseStartValue(JSONObject object) throws JSONException {
        return object.getDouble("start_value");
    }

    private static boolean parseDefaultVisible(JSONObject jsonObject) throws JSONException {
        return jsonObject.getBoolean("default_visible");
    }

    private static List<SWEKRelatedEvents> parseRelatedEvents(JSONObject configJSON) throws JSONException {
        List<SWEKRelatedEvents> relatedEventsList = new ArrayList<>();
        JSONArray relatedEventsArray = configJSON.getJSONArray("related_events");
        for (int i = 0; i < relatedEventsArray.length(); i++) {
            relatedEventsList.add(parseRelatedEvent(relatedEventsArray.getJSONObject(i)));
        }
        return relatedEventsList;
    }

    private static SWEKRelatedEvents parseRelatedEvent(JSONObject jsonObject) throws JSONException {
        return new SWEKRelatedEvents(parseRelatedEventName(jsonObject), parseRelatedWith(jsonObject), parseRelatedOnList(jsonObject));
    }

    private static SWEKEventType parseRelatedEventName(JSONObject jsonObject) throws JSONException {
        return eventTypes.get(jsonObject.getString("event_name"));
    }

    private static SWEKEventType parseRelatedWith(JSONObject jsonObject) throws JSONException {
        return eventTypes.get(jsonObject.getString("related_with"));
    }

    private static List<SWEKRelatedOn> parseRelatedOnList(JSONObject jsonObject) throws JSONException {
        List<SWEKRelatedOn> relatedOnList = new ArrayList<>();
        JSONArray relatedOnArray = jsonObject.getJSONArray("related_on");
        for (int i = 0; i < relatedOnArray.length(); i++) {
            relatedOnList.add(parseRelatedOn(relatedOnArray.getJSONObject(i)));
        }
        return relatedOnList;
    }

    private static SWEKRelatedOn parseRelatedOn(JSONObject jsonObject) throws JSONException {
        return new SWEKRelatedOn(parseParameterFrom(jsonObject), parseParameterWith(jsonObject), parseDbType(jsonObject));
    }

    private static SWEKParameter parseParameterFrom(JSONObject jsonObject) throws JSONException {
        String parameterName = jsonObject.getString("parameter_from");
        return new SWEKParameter(parameterName, parameterName, null, false);
    }

    private static SWEKParameter parseParameterWith(JSONObject jsonObject) throws JSONException {
        String parameterName = jsonObject.getString("parameter_with");
        return new SWEKParameter(parameterName, parameterName, null, false);
    }

}
