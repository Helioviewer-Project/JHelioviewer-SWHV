package org.helioviewer.jhv.plugins.swek.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.data.datatype.event.SWEKConfiguration;
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
import org.helioviewer.jhv.plugins.swek.view.SWEKIconBank;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SWEKConfigurationManager {

    private static SWEKConfigurationManager singletonInstance;

    private static final String configFileName = "SWEKSettings.json";

    private boolean configLoaded;

    private URL configFileURL;

    private final Map<String, SWEKSource> sources;

    private final Map<String, SWEKEventType> eventTypes;

    private final List<SWEKEventType> orderedEventTypes;

    private SWEKConfigurationManager() {
        configLoaded = false;
        sources = new HashMap<String, SWEKSource>();
        eventTypes = new HashMap<String, SWEKEventType>();
        orderedEventTypes = new ArrayList<SWEKEventType>();
    }

    public static SWEKConfigurationManager getSingletonInstance() {
        if (singletonInstance == null) {
            singletonInstance = new SWEKConfigurationManager();
        }
        return singletonInstance;
    }

    public void loadConfiguration() {
        if (!configLoaded) {
            Log.debug("search and open the configuration file");
            boolean isConfigParsed;
            if (checkAndOpenUserSetFile()) {
                isConfigParsed = parseConfigFile();
            } else if (checkAndOpenHomeDirectoryFile()) {
                boolean manuallyChanged = isManuallyChanged();
                if (!manuallyChanged) {
                    // check if the file is manually changed if not we download the latest version anyway
                    if (checkAndOpenZippedFile()) {
                        isConfigParsed = parseConfigFile();
                    } else {
                        isConfigParsed = false;
                    }
                } else {
                    isConfigParsed = parseConfigFile();
                }
            } else if (checkAndOpenZippedFile()) {
                isConfigParsed = parseConfigFile();
            } else {
                isConfigParsed = false;
            }
            configLoaded = isConfigParsed;
        }
    }

    private boolean isManuallyChanged() {
        try {
            Log.debug("configURL: " + configFileURL);
            JSONObject configJSON = JSONUtils.getJSONStream(configFileURL.openStream());
            return parseManuallyChanged(configJSON);
        } catch (JSONException e) {
            Log.error("Could not parse JSON: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            Log.error("Could not load the file: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public List<SWEKEventType> getOrderedEventTypes() {
        loadConfiguration();
        return orderedEventTypes;
    }

    private boolean checkAndOpenZippedFile() {
        URL url = SWEKPlugin.class.getResource("/" + configFileName);
        ReadableByteChannel rbc;
        try {
            rbc = Channels.newChannel(url.openStream());
            String saveFile = JHVDirectory.SETTINGS.getPath() + configFileName;
            FileOutputStream fos = new FileOutputStream(saveFile);
            try {
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            } finally {
                fos.close();
            }
            configFileURL = (new File(saveFile)).toURI().toURL();
            return true;
        } catch (IOException e) {
            Log.debug("Something went wrong extracting the configuration file from the jar bundle or saving it: " + e);
        }
        return false;
    }

    private boolean checkAndOpenHomeDirectoryFile() {
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

    private boolean checkAndOpenUserSetFile() {
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

    private boolean parseConfigFile() {
        try {
            JSONObject configJSON = JSONUtils.getJSONStream(configFileURL.openStream());
            EventDatabase.config_hash = Arrays.hashCode(configJSON.toString().toCharArray());
            SWEKConfiguration config = new SWEKConfiguration(parseVersion(configJSON), parseManuallyChanged(configJSON), parseSources(configJSON), parseEventTypes(configJSON), parseRelatedEvents(configJSON));
            SWEKEventType.setSwekRelatedEvents(config.getRelatedEvents());
            return true;
        } catch (IOException e) {
            Log.debug("Configuration file could not be parsed: " + e);
        } catch (JSONException e) {
            Log.debug("Could not parse config JSON: " + e);
        }
        return false;
    }

    private boolean parseManuallyChanged(JSONObject configJSON) throws JSONException {
        return configJSON.getBoolean("manually_changed");
    }

    private String parseVersion(JSONObject configJSON) throws JSONException {
        return configJSON.getString("config_version");
    }

    private List<SWEKSource> parseSources(JSONObject configJSON) throws JSONException {
        ArrayList<SWEKSource> swekSources = new ArrayList<SWEKSource>();
        JSONArray sourcesArray = configJSON.getJSONArray("sources");
        for (int i = 0; i < sourcesArray.length(); i++) {
            SWEKSource source = parseSource(sourcesArray.getJSONObject(i));
            if (source != null) {
                sources.put(source.getSourceName(), source);
                swekSources.add(source);
            }
        }
        return swekSources;
    }

    private SWEKSource parseSource(JSONObject jsonObject) throws JSONException {
        String name = parseSourceName(jsonObject);

        if (name.equals("HEK"))
            return new SWEKSource(name, parseGeneralParameters(jsonObject), new HEKParser(), new HEKDownloader());
        else if (name.equals("COMESEP"))
            return new SWEKSource(name, parseGeneralParameters(jsonObject), new ComesepParser(), new ComesepDownloader());
        else
            return null;
    }

    private String parseSourceName(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("name");
    }

    private List<SWEKParameter> parseGeneralParameters(JSONObject jsonObject) throws JSONException {
        List<SWEKParameter> parameterList = new ArrayList<SWEKParameter>();
        JSONArray parameterArray = jsonObject.getJSONArray("general_parameters");
        for (int i = 0; i < parameterArray.length(); i++) {
            parameterList.add(parseParameter(parameterArray.getJSONObject(i)));
        }
        return parameterList;
    }

    private List<SWEKEventType> parseEventTypes(JSONObject configJSON) throws JSONException {
        List<SWEKEventType> result = new ArrayList<SWEKEventType>();
        JSONArray eventJSONArray = configJSON.getJSONArray("events_types");
        for (int i = 0; i < eventJSONArray.length(); i++) {
            SWEKEventType eventType = parseEventType(eventJSONArray.getJSONObject(i));
            result.add(eventType);
            eventTypes.put(eventType.getEventName(), eventType);
            orderedEventTypes.add(eventType);
        }
        return result;
    }

    private SWEKEventType parseEventType(JSONObject object) throws JSONException {
        return new SWEKEventType(parseEventName(object), parseSuppliers(object), parseParameterList(object), parseEventIcon(object));
    }

    private ImageIcon parseEventIcon(JSONObject object) throws JSONException {
        String eventIconValue = object.getString("icon");
        try {
            URI eventIconURI = new URI(eventIconValue);
            if (eventIconURI.getScheme().toLowerCase().equals("iconbank")) {
                return SWEKIconBank.getSingletonInstance().getIcon(eventIconURI.getHost());
            } else {
                return SWEKIconBank.getSingletonInstance().getIcon("Other");
            }
        } catch (URISyntaxException e) {
            Log.info("Could not parse the URI " + eventIconValue + ", null icon returned");
        }
        return null;
    }

    private String parseEventName(JSONObject object) throws JSONException {
        return object.getString("event_name");
    }

    private List<SWEKSupplier> parseSuppliers(JSONObject object) throws JSONException {
        List<SWEKSupplier> suppliers = new ArrayList<SWEKSupplier>();
        JSONArray suppliersArray = object.getJSONArray("suppliers");
        for (int i = 0; i < suppliersArray.length(); i++) {
            suppliers.add(parseSupplier(suppliersArray.getJSONObject(i)));
        }
        return suppliers;
    }

    private SWEKSupplier parseSupplier(JSONObject object) throws JSONException {
        return new SWEKSupplier(parseSupplierName(object), parseSupplierDisplayName(object), parseSupplierSource(object), parseDbName(object));
    }

    private String parseSupplierName(JSONObject object) throws JSONException {
        return object.getString("supplier_name");
    }

    private String parseSupplierDisplayName(JSONObject object) throws JSONException {
        return object.getString("supplier_display_name");
    }

    private SWEKSource parseSupplierSource(JSONObject object) throws JSONException {
        return sources.get(object.getString("source"));
    }

    private String parseDbName(JSONObject object) throws JSONException {
        return object.getString("db");
    }

    private List<SWEKParameter> parseParameterList(JSONObject object) throws JSONException {
        List<SWEKParameter> parameterList = new ArrayList<SWEKParameter>();
        JSONArray parameterListArray = object.getJSONArray("parameter_list");
        for (int i = 0; i < parameterListArray.length(); i++) {
            parameterList.add(parseParameter((JSONObject) parameterListArray.get(i)));
        }
        return parameterList;
    }

    private SWEKParameter parseParameter(JSONObject jsonObject) throws JSONException {
        return new SWEKParameter(parseSourceInParameter(jsonObject), parseParameterName(jsonObject), parseParameterDisplayName(jsonObject), parseParameterFilter(jsonObject), parseDefaultVisible(jsonObject));
    }

    private String parseSourceInParameter(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("source");
    }

    private String parseParameterName(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("parameter_name");
    }

    private String parseParameterDisplayName(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("parameter_display_name");
    }

    private SWEKParameterFilter parseParameterFilter(JSONObject jsonObject) throws JSONException {
        JSONObject filterobject = jsonObject.optJSONObject("filter");
        if (filterobject != null) {
            return new SWEKParameterFilter(parseFilterType(filterobject), parseMin(filterobject), parseMax(filterobject), parseStartValue(filterobject), parseStepSize(filterobject), parseUnits(filterobject), parseDbType(filterobject));
        }
        return null;
    }

    private String parseDbType(JSONObject filterobject) throws JSONException {
        return filterobject.getString("dbtype");
    }

    private String parseUnits(JSONObject filterobject) throws JSONException {
        return filterobject.getString("units");
    }

    private String parseFilterType(JSONObject object) throws JSONException {
        return object.getString("filter_type");
    }

    private double parseMin(JSONObject object) throws JSONException {
        return object.getDouble("min");
    }

    private double parseMax(JSONObject object) throws JSONException {
        return object.getDouble("max");
    }

    private double parseStepSize(JSONObject object) throws JSONException {
        return object.getDouble("step_size");
    }

    private Double parseStartValue(JSONObject object) throws JSONException {
        return object.getDouble("start_value");
    }

    private boolean parseDefaultVisible(JSONObject jsonObject) throws JSONException {
        return jsonObject.getBoolean("default_visible");
    }

    private List<SWEKRelatedEvents> parseRelatedEvents(JSONObject configJSON) throws JSONException {
        List<SWEKRelatedEvents> relatedEventsList = new ArrayList<SWEKRelatedEvents>();
        JSONArray relatedEventsArray = configJSON.getJSONArray("related_events");
        for (int i = 0; i < relatedEventsArray.length(); i++) {
            relatedEventsList.add(parseRelatedEvent(relatedEventsArray.getJSONObject(i)));
        }
        return relatedEventsList;
    }

    private SWEKRelatedEvents parseRelatedEvent(JSONObject jsonObject) throws JSONException {
        return new SWEKRelatedEvents(parseRelatedEventName(jsonObject), parseRelatedWith(jsonObject), parseRelatedOnList(jsonObject));
    }

    private SWEKEventType parseRelatedEventName(JSONObject jsonObject) throws JSONException {
        return eventTypes.get(jsonObject.getString("event_name"));
    }

    private SWEKEventType parseRelatedWith(JSONObject jsonObject) throws JSONException {
        return eventTypes.get(jsonObject.getString("related_with"));
    }

    private List<SWEKRelatedOn> parseRelatedOnList(JSONObject jsonObject) throws JSONException {
        List<SWEKRelatedOn> relatedOnList = new ArrayList<SWEKRelatedOn>();
        JSONArray relatedOnArray = jsonObject.getJSONArray("related_on");
        for (int i = 0; i < relatedOnArray.length(); i++) {
            relatedOnList.add(parseRelatedOn(relatedOnArray.getJSONObject(i)));
        }
        return relatedOnList;
    }

    private SWEKRelatedOn parseRelatedOn(JSONObject jsonObject) throws JSONException {
        return new SWEKRelatedOn(parseParameterFrom(jsonObject), parseParameterWith(jsonObject), parseDbType(jsonObject));
    }

    private SWEKParameter parseParameterFrom(JSONObject jsonObject) throws JSONException {
        String parameterName = jsonObject.getString("parameter_from");
        return new SWEKParameter("", parameterName, parameterName, null, false);
    }

    private SWEKParameter parseParameterWith(JSONObject jsonObject) throws JSONException {
        String parameterName = jsonObject.getString("parameter_with");
        return new SWEKParameter("", parameterName, parameterName, null, false);
    }

}
